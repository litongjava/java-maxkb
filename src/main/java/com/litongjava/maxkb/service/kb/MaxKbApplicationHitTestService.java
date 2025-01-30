package com.litongjava.maxkb.service.kb;

import java.util.Arrays;
import java.util.List;

import com.jfinal.kit.Kv;
import com.litongjava.db.TableInput;
import com.litongjava.db.TableResult;
import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.kit.RowUtils;
import com.litongjava.maxkb.constant.TableNames;
import com.litongjava.model.result.ResultVo;
import com.litongjava.openai.constants.OpenAiModels;
import com.litongjava.table.services.ApiTable;
import com.litongjava.template.SqlTemplates;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MaxKbApplicationHitTestService {

  public ResultVo hitTest(Long userId, Long applicationId, String query_text, Double similarity, Integer top_number, String search_mode) {
    if ("embedding".equals(search_mode)) {
      return embeddingSearch(userId, applicationId, query_text, similarity, top_number);
    } else if ("blend".equals(search_mode)) {
      return blendSearch(userId, applicationId, query_text, similarity, top_number);
    }
    return null;
  }

  private ResultVo blendSearch(Long userId, Long applicationId, String query_text, Double similarity, Integer top_number) {
    // 获取数据集信息
    TableInput tableInput = new TableInput();
    tableInput.set("id", applicationId);
    if (userId.equals(1L)) {

    } else {
      tableInput.set("user_id", userId);
    }

    TableResult<Row> datasetResult = ApiTable.get(TableNames.max_kb_dataset, tableInput);

    Row dataset = datasetResult.getData();
    // 获取模型名称
    Long embeddingModeId = dataset.getLong("embedding_mode_id");
    String modelName = null;
    if (embeddingModeId != null) {
      modelName = Db.queryStr(String.format("SELECT model_name FROM %s WHERE id = ?", TableNames.max_kb_model), embeddingModeId);
      if (modelName == null) {
        modelName = OpenAiModels.text_embedding_3_large;
      }

    } else {
      modelName = OpenAiModels.text_embedding_3_large;
    }

    String sql = SqlTemplates.get("kb.list_database_id_by_application_id");
    List<Long> datasetIds = Db.queryListLong(sql, applicationId);
    if (datasetIds.size() < 1) {
      return ResultVo.fail("not found database of application id:", applicationId);
    }
    Long vectorId = Aop.get(MaxKbEmbeddingService.class).getVectorId(query_text, modelName);

    sql = SqlTemplates.get("kb.search_sentense_related_paragraph__with_dataset_ids");
    Long[] array = datasetIds.toArray(new Long[0]);
    List<Row> records = Db.find(sql, vectorId, array, similarity, top_number);
    List<Kv> kvs = RowUtils.recordsToKv(records, false);
    return ResultVo.ok(kvs);
  }

  private ResultVo embeddingSearch(Long userId, Long applicationId, String query_text, Double similarity, Integer top_number) {
    // 获取数据集信息
    TableInput tableInput = new TableInput();
    tableInput.set("id", applicationId);
    if (userId.equals(1L)) {

    } else {
      tableInput.set("user_id", userId);
    }

    TableResult<Row> datasetResult = ApiTable.get(TableNames.max_kb_dataset, tableInput);

    Row dataset = datasetResult.getData();
    // 获取模型名称
    String modelName = null;
    Long embeddingModeId = null;
    if (dataset != null) {
      embeddingModeId = dataset.getLong("embedding_mode_id");
      if (embeddingModeId != null) {
        modelName = Db.queryStr(String.format("SELECT model_name FROM %s WHERE id = ?", TableNames.max_kb_model), embeddingModeId);
      }
      if (modelName == null) {
        modelName = OpenAiModels.text_embedding_3_large;
      }

    } else {
      modelName = OpenAiModels.text_embedding_3_large;
    }

    String sql = SqlTemplates.get("kb.list_database_id_by_application_id");
    List<Long> datasetIds = Db.queryListLong(sql, applicationId);
    if (datasetIds.size() < 1) {
      return ResultVo.fail("not found database of application id:", applicationId);
    }
    Long vectorId = Aop.get(MaxKbEmbeddingService.class).getVectorId(query_text, modelName);
    //String ids = datasetIds.stream().map(id -> "?").collect(Collectors.joining(", "));

    //    sql = SqlTemplates.get("kb.hit_test_by_dataset_ids_with_max_kb_embedding_cache");
    //    sql = sql.replace("#(in_list)", ids);
    //
    //    int paramSize = 3 + datasetIds.size();
    //    Object[] params = new Object[paramSize];
    //
    //    params[0] = vectorId;
    //    for (int i = 0; i < datasetIds.size(); i++) {
    //      params[i + 1] = datasetIds.get(0);
    //    }
    //    params[paramSize - 2] = similarity;
    //    params[paramSize - 1] = top_number;
    //    log.info("sql:{},params:{}", sql, params);
    //    List<Row> records = Db.find(sql, params);

    sql = SqlTemplates.get("kb.search_sentense_related_paragraph__with_dataset_ids");
    Long[] array = datasetIds.toArray(new Long[0]);
    List<Row> records = Db.find(sql, vectorId, array, similarity, top_number);

    log.info("search_paragraph:{},{},{},{},{}", vectorId, Arrays.toString(array), similarity, top_number, records.size());

    List<Kv> kvs = RowUtils.recordsToKv(records, false);
    return ResultVo.ok(kvs);
  }

}
