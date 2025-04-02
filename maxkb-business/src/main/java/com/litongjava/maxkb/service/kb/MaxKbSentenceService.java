package com.litongjava.maxkb.service.kb;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;

import org.postgresql.util.PGobject;

import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.constant.MaxKbTableNames;
import com.litongjava.maxkb.model.MaxKbSentence;
import com.litongjava.maxkb.utils.ExecutorServiceUtils;
import com.litongjava.openai.token.OpenAiTokenizer;
import com.litongjava.tio.utils.crypto.Md5Utils;
import com.litongjava.tio.utils.snowflake.SnowflakeIdUtils;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MaxKbSentenceService {
  MaxKbEmbeddingService maxKbEmbeddingService = Aop.get(MaxKbEmbeddingService.class);
  CompletionService<MaxKbSentence> completionServiceSentence = new ExecutorCompletionService<>(ExecutorServiceUtils.getExecutorService());

  CompletionService<Row> completionServiceRow = new ExecutorCompletionService<>(ExecutorServiceUtils.getExecutorService());
  MaxKbParagraphSummaryService maxKbParagraphSummaryService = Aop.get(MaxKbParagraphSummaryService.class);

  public boolean summaryToSentenceAndSave(Long dataset_id, String modelName, List<Row> paragraphRecords, Long documentIdFinal) {
    boolean transactionSuccess;

    // Step 1: Generate summaries asynchronously
    List<Future<MaxKbSentence>> summaryFutures = new ArrayList<>(paragraphRecords.size());

    for (Row paragraph : paragraphRecords) {
      Future<MaxKbSentence> future = completionServiceSentence.submit(() -> {
        String paragraphContent = paragraph.getStr("content");
        // Generate summary for the paragraph
        String sentenceContent = maxKbParagraphSummaryService.summary(paragraphContent);

        // Create MaxKbSentence object with the summary
        MaxKbSentence maxKbSentence = new MaxKbSentence();
        maxKbSentence.setId(SnowflakeIdUtils.id()).setType(2) // Assuming type 2 indicates a summary
            .setHitNum(0).setMd5(Md5Utils.getMD5(sentenceContent)).setContent(sentenceContent)
            //
            .setDatasetId(dataset_id).setDocumentId(documentIdFinal).setParagraphId(paragraph.getLong("id"));

        return maxKbSentence;
      });
      summaryFutures.add(future);
    }

    // Step 2: Retrieve summaries from futures
    List<MaxKbSentence> sentences = new ArrayList<>();
    for (Future<MaxKbSentence> future : summaryFutures) {
      try {
        MaxKbSentence sentence = future.get(); // This will block until the summary is ready
        if (sentence != null) {
          sentences.add(sentence);
        }
      } catch (Exception e) {
        log.error("Error generating summary: {}", e.getMessage(), e);
      }
    }

    // Step 3: Generate vectors asynchronously for each summary
    List<Future<Row>> vectorFutures = new ArrayList<>(sentences.size());
    for (MaxKbSentence sentence : sentences) {
      Future<Row> future = completionServiceRow.submit(() -> {
        // Generate vector for the summary content
        PGobject vector = maxKbEmbeddingService.getVector(sentence.getContent(), modelName);
        Row record = sentence.toRow();
        record.set("embedding", vector);
        return record;
      });
      vectorFutures.add(future);
    }

    // Step 4: Retrieve vectors from futures
    List<Row> sentenceRows = new ArrayList<>();
    for (Future<Row> future : vectorFutures) {
      try {
        Row record = future.get(); // This will block until the vector is ready
        if (record != null) {
          sentenceRows.add(record);
        }
      } catch (Exception e) {
        log.error("Error generating vector: {}", e.getMessage(), e);
      }
    }

    // Step 5: Save all sentence records with embeddings to the database within a transaction
    transactionSuccess = Db.tx(() -> {
      // Delete existing sentences for the document to avoid duplicates
      Db.deleteById(MaxKbTableNames.max_kb_sentence, "document_id", documentIdFinal);
      // Batch save the new sentences with embeddings
      Db.batchSave(MaxKbTableNames.max_kb_sentence, sentenceRows, 2000);
      return true;
    });

    return transactionSuccess;
  }

  public boolean splitToSentenceAndSave(Long dataset_id, String modelName, List<Row> paragraphRecords, Long documentIdFinal) {
    boolean transactionSuccess;
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
      futures.add(completionServiceRow.submit(() -> {
        PGobject vector = maxKbEmbeddingService.getVector(sentence.getContent(), modelName);
        Row record = sentence.toRow();
        record.set("embedding", vector);
        return record;
      }));
    }

    List<Row> sentenceRows = new ArrayList<>();
    for (int i = 0; i < sentences.size(); i++) {
      try {
        Future<Row> future = completionServiceRow.take();
        Row record = future.get();
        if (record != null) {
          sentenceRows.add(record);
        }
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      }
    }

    transactionSuccess = Db.tx(() -> {
      Db.deleteById(MaxKbTableNames.max_kb_sentence, "document_id", documentIdFinal);
      Db.batchSave(MaxKbTableNames.max_kb_sentence, sentenceRows, 2000);
      return true;
    });
    return transactionSuccess;
  }
}
