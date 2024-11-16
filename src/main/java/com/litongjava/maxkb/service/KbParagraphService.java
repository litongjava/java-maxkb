package com.litongjava.maxkb.service;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.postgresql.util.PGobject;

import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Record;
import com.litongjava.db.utils.PgVectorUtils;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.client.RumiClient;
import com.litongjava.maxkb.vo.KbParagraph;
import com.litongjava.tio.utils.UUIDUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KbParagraphService {

  /**
   * 查询所有的段落并返回
   * 
   * @return List<KbParagraph>
   */
  public List<KbParagraph> embedding() {
    List<KbParagraph> all = Db.findAll(KbParagraph.class, "paragraph");
    log.info("size:{}", all.size());
    return all;
  }

  public void reEmbedding() {
    // Step 1: 根据 paragraph_id 查询 paragraph 表的数据
    String paragraphQuery = "SELECT p.id,p.content, p.dataset_id, p.document_id FROM paragraph p";
    List<Record> records = Db.find(paragraphQuery);
    for (Record paragraphResult : records) {
      String content = paragraphResult.get("content");
      UUID datasetId = paragraphResult.get("dataset_id");
      UUID documentId = paragraphResult.get("document_id");
      UUID paragraphId = paragraphResult.get("id");
      log.info("Paragraph content: {}, datasetId: {}, documentId: {}", content, datasetId, documentId);

      // Step 2: 根据 dataset_id 或 document_id 来查询 model 表的详细信息

      // Step 3: 调用 embedding 远程服务
      String embeddingString = Aop.get(RumiClient.class).embedding(content);
      log.info("Embedding result: {}", embeddingString);
      PGobject pgVector = PgVectorUtils.getPgVector(embeddingString);

      Record embedding = new Record();
      embedding.put("id", UUIDUtils.random().toString());
      embedding.set("source_id", paragraphId.toString()).set("source_type", 1);
      embedding.set("is_active", true);
      embedding.set("embedding", pgVector);
      embedding.set("dataset_id", datasetId).set("document_id", documentId).set("paragraph_id", paragraphId);
      PGobject meta = new PGobject();
      try {
        meta.setType("jsonb");
        meta.setValue("{}");
      } catch (SQLException e) {
        e.printStackTrace();
      }
      embedding.set("meta", meta);
      PGobject search_vector = new PGobject();

      search_vector.setType("tsvector");
      try {
        search_vector.setValue("");
      } catch (SQLException e1) {
        e1.printStackTrace();
      }
      embedding.set("search_vector", search_vector);

//      try {
        //boolean ok = Db.save("embedding", embedding);
//        if (!ok) {
//          log.error("Failed to svae db:{},{}", embedding, paragraphId.toString());
//        } else {
//          log.info("Successful save to db");
//        }
//      } catch (Exception e) {
//        e.printStackTrace();
//      }
    }

  }

  /**
   * 根据 paragraph_id 查询 embedding，并调用远程服务
   * 
   * @param id
   */
  public void embedding(String id) {
    // Step 1: 根据 paragraph_id 查询 paragraph 表的数据
    String paragraphQuery = "SELECT p.content, p.dataset_id, p.document_id FROM paragraph p WHERE p.id = ?";
    Record paragraphResult = Db.findFirst(paragraphQuery, UUID.fromString(id));
    if (paragraphResult == null) {
      log.warn("No paragraph found for id: {}", id);
      return;
    }

    String content = paragraphResult.get("content");
    UUID datasetId = paragraphResult.get("dataset_id");
    UUID documentId = paragraphResult.get("document_id");

    log.info("Paragraph content: {}, datasetId: {}, documentId: {}", content, datasetId, documentId);

    // Step 2: 根据 dataset_id 或 document_id 来查询 model 表的详细信息
    /**
     * SELECT m.model_name,m.provider FROM model m JOIN dataset d ON
     * d.embedding_mode_id = m.id JOIN paragraph p ON p.dataset_id = d.id WHERE p.id
     * = '2c66eeea-6c9d-11ef-a830-706655b928b8';
     */

    // Step 3: 调用 embedding 远程服务
    String embeddingString = Aop.get(RumiClient.class).embedding(content);
    log.info("Embedding result: {}", embeddingString);
    PGobject pgVector = PgVectorUtils.getPgVector(embeddingString);

    Record embedding = new Record();
    embedding.put("id", UUIDUtils.random().toString());
    embedding.set("source_id", id).set("source_type", 1);
    embedding.set("is_active", true);
    embedding.set("embedding", pgVector);
    embedding.set("dataset_id", datasetId).set("document_id", documentId).set("paragraph_id", UUID.fromString(id));
    PGobject meta = new PGobject();
    try {
      meta.setType("jsonb");
      meta.setValue("{}");
    } catch (SQLException e) {
      e.printStackTrace();
    }
    embedding.set("meta", meta);
    PGobject search_vector = new PGobject();

    search_vector.setType("tsvector");
    try {
      search_vector.setValue("");
    } catch (SQLException e1) {
      e1.printStackTrace();
    }
    embedding.set("search_vector", search_vector);

    try {
      boolean ok = Db.save("embedding", embedding);
      if (!ok) {
        log.error("Failed to svae db:{},{}", embedding, id);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
