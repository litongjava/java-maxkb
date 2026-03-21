package com.litongjava.maxkb.service.kb;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.maxkb.vo.MaxKbRetrievalResult;

public class KbRetrievalService {

  public List<Row> retrieval(String sql, Long vectorId, Long datasetId, Double similarity, Integer top_number) {
    List<Row> records = Db.find(sql, vectorId, datasetId, similarity, top_number);
    return records;
  }

  public List<MaxKbRetrievalResult> retrievalParagraph(String sql, Long vectorId, Long datasetId, Double similarity,
      Integer top_number) {
    List<Row> records = Db.find(sql, vectorId, datasetId, similarity, top_number);
    List<MaxKbRetrievalResult> results = null;
    if (records != null && records.size() > 0) {
      results = new ArrayList<>();
      for (Row row : records) {
        Long id = row.getLong("id");
        Long dataset_id = row.getLong("dataset_id");
        Long document_id = row.getLong("document_id");

        String dataset_name = row.getStr("dataset_name");
        String document_name = row.getStr("document_name");

        String title = row.getStr("title");
        String content = row.getStr("content");

        Double output_similarity = row.getDouble("similarity");
        Double comprehensive_score = row.getDouble("comprehensive_score");

        String status = row.getStr("status");
        Boolean is_active = row.getBoolean("is_active");
        int hit_num = row.getInt("hit_num");
        Timestamp create_time = row.getTimestamp("create_time");
        Timestamp update_time = row.getTimestamp("update_time");
        MaxKbRetrievalResult maxKbRetrievalResult = new MaxKbRetrievalResult(id, dataset_id, document_id,
            //
            dataset_name, document_name, title, content,
            //
            output_similarity, comprehensive_score,
            //
            status, is_active, hit_num, create_time, update_time);
        results.add(maxKbRetrievalResult);
      }
    }
    return results;
  }

}
