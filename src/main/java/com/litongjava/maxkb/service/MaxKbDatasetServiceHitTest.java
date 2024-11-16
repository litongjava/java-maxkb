package com.litongjava.maxkb.service;

import java.util.List;

import com.jfinal.kit.Kv;
import com.litongjava.db.TableInput;
import com.litongjava.db.TableResult;
import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Record;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.kit.RecordUtils;
import com.litongjava.maxkb.constant.TableNames;
import com.litongjava.model.result.ResultVo;
import com.litongjava.openai.constants.OpenAiModels;
import com.litongjava.table.services.ApiTable;
import com.litongjava.template.SqlTemplates;

public class MaxKbDatasetServiceHitTest {

  public ResultVo hitTest(Long userId, Long datasetId, String query_text, Double similarity, Integer top_number, String search_mode) {
    if ("embedding".equals(search_mode)) {
      // 获取数据集信息
      TableInput tableInput = new TableInput();
      tableInput.set("id", datasetId);
      if (userId.equals(1L)) {

      } else {
        tableInput.set("user_id", userId);
      }

      TableResult<Record> datasetResult = ApiTable.get(TableNames.max_kb_dataset, tableInput);

      Record dataset = datasetResult.getData();
      // 获取模型名称
      Long embeddingModeId = dataset.getLong("embedding_mode_id");
      String modelName = null;
      if (embeddingModeId != null) {
        modelName = Db.queryStr(String.format("SELECT model_name FROM %s WHERE id = ?", TableNames.max_kb_model), embeddingModeId);
        if(modelName==null) {
          modelName = OpenAiModels.text_embedding_3_large;
        }
      
      } else {
        modelName = OpenAiModels.text_embedding_3_large;
      }

      String sql = SqlTemplates.get("kb.hit_test_by_dataset_id_with_max_kb_embedding_cache");
      Long vectorId = Aop.get(MaxKbEmbeddingService.class).getVectorId(query_text, modelName);
      List<Record> records = Db.find(sql, vectorId, datasetId, similarity, top_number);
      List<Kv> kvs = RecordUtils.recordsToKv(records, false);
      return ResultVo.ok(kvs);
    }
    return null;
  }

}
