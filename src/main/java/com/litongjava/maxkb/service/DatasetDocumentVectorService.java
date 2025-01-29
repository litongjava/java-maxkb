package com.litongjava.maxkb.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;

import org.postgresql.util.PGobject;

import com.jfinal.kit.Kv;
import com.litongjava.db.TableInput;
import com.litongjava.db.TableResult;
import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.constant.TableNames;
import com.litongjava.maxkb.model.MaxKbSentence;
import com.litongjava.maxkb.utils.ExecutorServiceUtils;
import com.litongjava.maxkb.vo.DocumentBatchVo;
import com.litongjava.maxkb.vo.Paragraph;
import com.litongjava.model.result.ResultVo;
import com.litongjava.table.services.ApiTable;
import com.litongjava.tio.utils.crypto.Md5Utils;
import com.litongjava.tio.utils.hutool.FilenameUtils;
import com.litongjava.tio.utils.snowflake.SnowflakeIdUtils;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DatasetDocumentVectorService {

  MaxKbEmbeddingService maxKbEmbeddingService = Aop.get(MaxKbEmbeddingService.class);

  public ResultVo batch(Long userId, Long dataset_id, List<DocumentBatchVo> list) {

    TableInput tableInput = new TableInput();
    tableInput.set("id", dataset_id);
    if (!userId.equals(1L)) {
      tableInput.set("user_id", userId);
    }

    TableResult<Row> result = ApiTable.get(TableNames.max_kb_dataset, tableInput);

    Row dataset = result.getData();
    if (dataset == null) {
      return ResultVo.fail("Dataset not found.");
    }

    Long embedding_mode_id = dataset.getLong("embedding_mode_id");
    String sqlModelName = String.format("SELECT model_name FROM %s WHERE id = ?", TableNames.max_kb_model);
    String modelName = Db.queryStr(sqlModelName, embedding_mode_id);
    String sqlDocumentId = String.format("SELECT id FROM %s WHERE user_id = ? AND file_id = ?", TableNames.max_kb_document);

    List<Kv> kvs = new ArrayList<>();
    CompletionService<Row> completionService = new ExecutorCompletionService<>(ExecutorServiceUtils.getExecutorService());

    for (DocumentBatchVo documentBatchVo : list) {
      Long fileId = documentBatchVo.getId();
      String filename = documentBatchVo.getName();
      Long documentId = Db.queryLong(sqlDocumentId, userId, fileId);

      List<Paragraph> paragraphs = documentBatchVo.getParagraphs();
      int char_length = 0;
      int size = 0;
      if (paragraphs != null) {
        size = paragraphs.size();
        for (Paragraph p : paragraphs) {
          if (p.getContent() != null) {
            char_length += p.getContent().length();
          }
        }
      }

      String type = FilenameUtils.getSuffix(filename);

      if (documentId == null) {
        documentId = SnowflakeIdUtils.id();
        Row record = Row.by("id", documentId)
            //
            .set("file_id", fileId).set("user_id", userId).set("name", filename)
            //
            .set("char_length", char_length).set("status", "1").set("is_active", true)
            //
            .set("type", type).set("dataset_id", dataset_id).set("paragraph_count", size)
            //
            .set("hit_handling_method", "optimization").set("directly_return_similarity", 0.9);
        Db.save(TableNames.max_kb_document, record);
        Kv kv = record.toKv();
        kvs.add(kv);
      } else {
        Row existingRecord = Db.findById(TableNames.max_kb_document, documentId);
        if (existingRecord != null) {
          Kv kv = existingRecord.toKv();
          kvs.add(kv);
        } else {
          // Handle the case where documentId is provided but the record does not exist
          return ResultVo.fail("Document not found for ID: " + documentId);
        }
      }

      List<Row> paragraphRecords = new ArrayList<>();
      extraParagraph(dataset_id, fileId, documentId, paragraphs, type, paragraphRecords);

      Long documentIdFinal = documentId;
      boolean transactionSuccess = Db.tx(() -> {
        Db.delete(TableNames.max_kb_paragraph, Row.by("document_id", documentIdFinal));
        Db.batchSave(TableNames.max_kb_paragraph, paragraphRecords, 2000);
        return true;
      });

      if (!transactionSuccess) {
        return ResultVo.fail("Transaction failed while saving paragraphs for document ID: " + documentIdFinal);
      }

      List<MaxKbSentence> sentences = new ArrayList<>();
      for (Row paragraph : paragraphRecords) {
        String paragraphContent = paragraph.getStr("content");

        //继续拆分片段 为句子 
        Document document = new Document(paragraphContent);
        // 使用较大的块大小（150）和相同的重叠（50）
        DocumentSplitter splitter = DocumentSplitters.recursive(150, 50, new OpenAiTokenizer());
        List<TextSegment> segments = splitter.split(document);
        for (TextSegment segment : segments) {
          String sentenceContent = segment.text();

          MaxKbSentence maxKbSentence = new MaxKbSentence();
          maxKbSentence.setId(SnowflakeIdUtils.id()).setType(1).setHitNum(0)
              //
              .setMd5(Md5Utils.getMD5(sentenceContent)).setContent(sentenceContent)
              //
              .setDatasetId(dataset_id).setDocumentId(documentIdFinal).setParagraphId(paragraph.getLong("id"));

          sentences.add(maxKbSentence);
        }

      }

      List<Future<Row>> futures = new ArrayList<>(sentences.size());
      for (MaxKbSentence sentence : sentences) {
        futures.add(completionService.submit(() -> {
          PGobject vector = maxKbEmbeddingService.getVector(sentence.getContent(), modelName);
          Row record = sentence.toRecord();
          record.set("embedding", vector);
          return record;
        }));
      }

      List<Row> sentenceRows = new ArrayList<>();
      for (int i = 0; i < sentences.size(); i++) {
        try {
          Future<Row> future = completionService.take();
          Row record = future.get();
          if (record != null) {
            sentenceRows.add(record);
          }
        } catch (Exception e) {
          log.error(e.getMessage(), e);
        }
      }

      transactionSuccess = Db.tx(() -> {
        Db.deleteById(TableNames.max_kb_sentence, "document_id", documentIdFinal);
        Db.batchSave(TableNames.max_kb_sentence, sentenceRows, 2000);
        return true;
      });

      if (!transactionSuccess) {
        return ResultVo.fail("Transaction failed while saving senttents for document ID: " + documentIdFinal);
      }

    }

    return ResultVo.ok(kvs);
  }

  private void extraParagraph(Long dataset_id, Long fileId, Long documentId, List<Paragraph> paragraphs, String type, List<Row> batchRecord) {
    if (paragraphs != null) {
      final long documentIdFinal = documentId;
      for (Paragraph p : paragraphs) {

        String title = p.getTitle();
        String content = p.getContent();

        Row row = Row.by("id", SnowflakeIdUtils.id())
            //
            .set("source_id", fileId)
            //
            .set("source_type", type)
            //
            .set("title", title)
            //
            .set("content", content)
            //
            .set("md5", Md5Utils.getMD5(content))
            //
            .set("status", "1")
            //
            .set("hit_num", 0)
            //
            .set("is_active", true).set("dataset_id", dataset_id).set("document_id", documentIdFinal);
        batchRecord.add(row);

      }
    }
  }
}
