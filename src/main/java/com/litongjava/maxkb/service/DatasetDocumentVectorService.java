package com.litongjava.maxkb.service;

import java.util.ArrayList;
import java.util.List;

import org.postgresql.util.PGobject;

import com.jfinal.kit.Kv;
import com.litongjava.db.TableInput;
import com.litongjava.db.TableResult;
import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Record;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.constant.TableNames;
import com.litongjava.maxkb.vo.DocumentBatchVo;
import com.litongjava.maxkb.vo.Paragraph;
import com.litongjava.model.result.ResultVo;
import com.litongjava.table.services.ApiTable;
import com.litongjava.tio.utils.crypto.Md5Utils;
import com.litongjava.tio.utils.hutool.FilenameUtils;
import com.litongjava.tio.utils.snowflake.SnowflakeIdUtils;

public class DatasetDocumentVectorService {

  public ResultVo batch(Long userId, Long dataset_id, List<DocumentBatchVo> list) {

    TableInput tableInput = new TableInput();
    tableInput.set("id", dataset_id);
    if (userId.equals(1L)) {

    } else {
      tableInput.set("user_id", userId);
    }

    TableResult<Record> result = ApiTable.get(TableNames.max_kb_dataset, tableInput);

    Record dataset = result.getData();
    Long embedding_mode_id = dataset.getLong("embedding_mode_id");
    String sql = String.format("select model_name from %s where id=?", TableNames.max_kb_model);
    String modelName = Db.queryStr(sql, embedding_mode_id);
    sql = String.format("select id from %s where user_id=? and file_id=?", TableNames.max_kb_document);

    List<Kv> kvs = new ArrayList<>();
    for (DocumentBatchVo documentBatchVo : list) {
      Long fileId = documentBatchVo.getId();
      String filename = documentBatchVo.getName();
      Long documentId = Db.queryLong(sql, userId, fileId);

      List<Paragraph> paragraphs = documentBatchVo.getParagraphs();
      int char_length = 0;
      for (Paragraph p : paragraphs) {
        int length = p.getContent().length();
        char_length += length;
      }
      String type = FilenameUtils.getSuffix(filename);
      if (documentId == null) {
        documentId = SnowflakeIdUtils.id();
        Record record = Record.by("id", documentId).set("file_id", fileId).set("user_id", userId).set("name", filename)
            //
            .set("char_length", char_length).set("status", "1").set("is_active", true).set("type", type)
            //
            .set("dataset_id", dataset_id).set("paragraph_count", paragraphs.size())
            //
            .set("hit_handling_method", "optimization").set("directly_return_similarity", 0.9);
        Db.save(TableNames.max_kb_document, record);
        Kv kv = record.toKv();
        kvs.add(kv);
      } else {
        Kv kv = Db.findById(TableNames.max_kb_document, documentId).toKv();
        kvs.add(kv);
      }

      //
      MaxKbEmbeddingService maxKbEmbeddingService = Aop.get(MaxKbEmbeddingService.class);

      List<Record> batchRecord = new ArrayList<>(paragraphs.size());
      for (Paragraph p : paragraphs) {
        String title = p.getTitle();
        String content = p.getContent();
        PGobject vector = maxKbEmbeddingService.getVector(content, modelName);
        Record record = Record.by("id", SnowflakeIdUtils.id()).set("source_id", fileId).set("source_type", type)
            //
            .set("title", title).set("content", p.getContent()).set("md5", Md5Utils.getMD5(content))
            //
            .set("status", "1").set("hit_num", 0).set("is_active", true)
            //
            .set("dataset_id", dataset_id).set("document_id", documentId)
            //
            .set("embedding", vector);

        batchRecord.add(record);
      }
      final long documentIdFianl = documentId;
      Db.tx(() -> {
        Db.delete(TableNames.max_kb_paragraph, Record.by("document_id", documentIdFianl));
        Db.batchSave(TableNames.max_kb_paragraph, batchRecord, 2000);
        return true;
      });
    }
    return ResultVo.ok(kvs);
  }

}
