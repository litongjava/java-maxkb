package nexus.io.maxkb.service.kb;

import java.util.Arrays;
import java.util.List;

import com.jfinal.kit.Kv;

import lombok.extern.slf4j.Slf4j;
import nexus.io.db.TableInput;
import nexus.io.db.TableResult;
import nexus.io.db.activerecord.Db;
import nexus.io.db.activerecord.Row;
import nexus.io.jfinal.aop.Aop;
import nexus.io.kit.RowUtils;
import nexus.io.maxkb.constant.MaxKbTableNames;
import nexus.io.model.result.ResultVo;
import nexus.io.openai.consts.OpenAiModels;
import nexus.io.table.services.ApiTable;
import nexus.io.template.SqlTemplates;

@Slf4j
public class MaxKbDatasetHitTestService {

  public ResultVo hitTest(Long userId, Long datasetId, String query_text, Double similarity, Integer top_number, String search_mode) {
    if ("embedding".equals(search_mode)) {
      return embeddingSearch(userId, datasetId, query_text, similarity, top_number);
    } else if ("blend".equals(search_mode)) {
      return blendSearch(userId, datasetId, query_text, similarity, top_number);
    }
    return null;
  }

  private ResultVo blendSearch(Long userId, Long datasetId, String query_text, Double similarity, Integer top_number) {
    // 获取数据集信息
    TableInput tableInput = new TableInput();
    tableInput.set("id", datasetId);
    if (userId.equals(1L)) {

    } else {
      tableInput.set("user_id", userId);
    }

    TableResult<Row> datasetResult = ApiTable.get(MaxKbTableNames.max_kb_dataset, tableInput);

    Row dataset = datasetResult.getData();
    // 获取模型名称
    Long embeddingModeId = dataset.getLong("embedding_mode_id");
    String modelName = null;
    if (embeddingModeId != null) {
      modelName = Db.queryStr(String.format("SELECT model_name FROM %s WHERE id = ?", MaxKbTableNames.max_kb_model), embeddingModeId);
      if (modelName == null) {
        modelName = OpenAiModels.TEXT_EMBEDDING_3_LARGE;
      }

    } else {
      modelName = OpenAiModels.TEXT_EMBEDDING_3_LARGE;
    }

    String sql = SqlTemplates.get("kb.hit_test_by_dataset_id_with_max_kb_embedding_cache");
    Long vectorId = Aop.get(KbEmbeddingService.class).getVectorId(query_text, modelName);
    List<Row> records = Db.find(sql, vectorId, datasetId, similarity, top_number);

    List<Kv> kvs = RowUtils.toKv(records, false);
    return ResultVo.ok(kvs);
  }

  private ResultVo embeddingSearch(Long userId, Long datasetId, String query_text, Double similarity, Integer top_number) {
    // 获取数据集信息
    TableInput tableInput = new TableInput();
    tableInput.set("id", datasetId);
    if (userId.equals(1L)) {

    } else {
      tableInput.set("user_id", userId);
    }

    TableResult<Row> datasetResult = ApiTable.get(MaxKbTableNames.max_kb_dataset, tableInput);

    Row dataset = datasetResult.getData();
    // 获取模型名称
    Long embeddingModeId = dataset.getLong("embedding_mode_id");
    String modelName = null;
    if (embeddingModeId != null) {
      modelName = Db.queryStr(String.format("SELECT model_name FROM %s WHERE id = ?", MaxKbTableNames.max_kb_model), embeddingModeId);
      if (modelName == null) {
        modelName = OpenAiModels.TEXT_EMBEDDING_3_LARGE;
      }

    } else {
      modelName = OpenAiModels.TEXT_EMBEDDING_3_LARGE;
    }

    Long vectorId = Aop.get(KbEmbeddingService.class).getVectorId(query_text, modelName);

    //String sql = SqlTemplates.get("kb.hit_test_by_dataset_id_with_max_kb_embedding_cache");
    //List<Row> records = Db.find(sql, vectorId, datasetId, similarity, top_number);

    String sql = SqlTemplates.get("kb.search_sentense_related_paragraph__with_dataset_ids");
    Long[] datasetIdArray = { datasetId };
    List<Row> records = Db.find(sql, vectorId, datasetIdArray, similarity, top_number);
    log.info("search_paragraph:{},{},{},{},{}", vectorId, Arrays.toString(datasetIdArray), similarity, top_number, records.size());

    List<Kv> kvs = RowUtils.toKv(records, false);
    return ResultVo.ok(kvs);
  }

}
