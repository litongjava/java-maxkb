package com.litongjava.maxkb.service.kb;

import java.util.ArrayList;
import java.util.List;

import com.jfinal.kit.Kv;
import com.litongjava.db.TableInput;
import com.litongjava.db.TableResult;
import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.kit.RowUtils;
import com.litongjava.maxkb.constant.MaxKbTableNames;
import com.litongjava.maxkb.dao.MaxKbDatasetDao;
import com.litongjava.maxkb.model.MaxKbDataset;
import com.litongjava.maxkb.vo.KbDatasetModel;
import com.litongjava.maxkb.vo.ResultPage;
import com.litongjava.model.page.Page;
import com.litongjava.model.result.ResultVo;
import com.litongjava.table.constants.Operators;
import com.litongjava.table.services.ApiTable;

public class MaxKbDatasetService {

  private String application_mapping_count_sql = String.format("select count(1) from %s where dataset_id=?", MaxKbTableNames.max_kb_application_dataset_mapping);
  private String document_count_sql = String.format("select count(1) from %s where dataset_id=?", MaxKbTableNames.max_kb_document);
  private String sum_char_length_sql = String.format("select sum(char_length) from %s where dataset_id=?", MaxKbTableNames.max_kb_document);

  public ResultVo page(Long userId, Integer pageNo, Integer pageSize, String name) {
    TableInput tableInput = new TableInput();
    tableInput.setPageNo(pageNo).setPageSize(pageSize);
    if (userId.equals(1L)) {

    } else {
      tableInput.set("user_id", userId);
    }

    if (name != null) {
      tableInput.set("name", name).set("name_op", Operators.CT);
    }

    TableResult<Page<Row>> tableResult = ApiTable.page(MaxKbTableNames.max_kb_dataset, tableInput);
    Page<Row> page = tableResult.getData();
    int totalRow = page.getTotalRow();
    List<Row> list = page.getList();
    List<Kv> kvs = new ArrayList<>();
    for (Row record : list) {
      Kv kv = record.toKv();
      Long datasetId = kv.getLong("id");

      Long application_mapping_count = Db.queryLong(application_mapping_count_sql, datasetId);
      kv.set("application_mapping_count", application_mapping_count);
      Long document_count = Db.queryLong(document_count_sql, datasetId);
      kv.set("document_count", document_count);
      Long charLength = Db.queryLong(sum_char_length_sql, datasetId);
      kv.set("char_length", charLength);
      kvs.add(kv);
    }

    ResultPage<Kv> resultPage = new ResultPage<>(pageNo, pageSize, totalRow, kvs);
    return ResultVo.ok(resultPage);
  }

  public ResultVo save(Long userId, KbDatasetModel kbDatasetModel) {
    ResultVo resultVo = new ResultVo();
    TableResult<Kv> saveOrUpdate = Aop.get(MaxKbDatasetDao.class).saveOrUpdate(userId, kbDatasetModel);
    Kv data = saveOrUpdate.getData();
    resultVo.setData(data);
    return resultVo;
  }

  public ResultVo get(Long userId, Long id) {
    ResultVo resultVo = new ResultVo();
    TableInput tableInput = new TableInput();
    if (userId != null && userId.equals(1L)) {
      tableInput.set("id", id);
    } else {
      tableInput.set("id", id).set("user_id", userId);
    }
    TableResult<Row> result = ApiTable.get(MaxKbTableNames.max_kb_dataset, tableInput);
    Kv kv = result.getData().toKv();
    resultVo.setData(kv);
    return resultVo;
  }

  public ResultVo list(Long userId) {
    Row queryRecord = new Row();
    if (userId.equals(1L)) {

    } else {
      queryRecord.set("user_id", userId);
    }

    String columns = "id,name,\"desc\",type,meta,user_id,embedding_mode_id,create_time,update_time";
    List<Row> records = Db.find(MaxKbDataset.tableName, columns, queryRecord);
    List<Kv> kvs = RowUtils.toKv(records, false);
    return ResultVo.ok(kvs);
  }

  public ResultVo delete(Long userId, Long id) {
    TableInput tableInput = null;
    if (userId != null && userId.equals(1L)) {
      tableInput = TableInput.by("id", id);
    } else {
      tableInput = TableInput.by("id", id).set("user_id", userId);
    }

    TableResult<Boolean> result = ApiTable.delete(MaxKbTableNames.max_kb_dataset, tableInput);
    Row deleteRecord = Row.by("dataset_id", id);
    Db.delete(MaxKbTableNames.max_kb_document, deleteRecord);
    Db.delete(MaxKbTableNames.max_kb_paragraph, deleteRecord);
    return new ResultVo(result.getData());
  }

  public ResultVo getApplicationByDatasetId(Long datasetId) {
    return ResultVo.ok();
  }
}
