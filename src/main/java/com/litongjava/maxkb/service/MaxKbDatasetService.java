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
import com.litongjava.maxkb.dao.MaxKbDatasetDao;
import com.litongjava.maxkb.model.MaxKbDataset;
import com.litongjava.maxkb.vo.KbDatasetModel;
import com.litongjava.maxkb.vo.ResultPage;
import com.litongjava.model.page.Page;
import com.litongjava.model.result.ResultVo;
import com.litongjava.table.constants.Operators;
import com.litongjava.table.services.ApiTable;

public class MaxKbDatasetService {

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

    TableResult<Page<Record>> tableResult = ApiTable.page(TableNames.max_kb_dataset, tableInput);
    Page<Record> page = tableResult.getData();
    int totalRow = page.getTotalRow();
    List<Kv> kvs = RecordUtils.recordsToKv(page.getList(), false);
    ResultPage<Kv> resultPage = new ResultPage<>(pageNo, pageSize, totalRow, kvs);
    return ResultVo.ok(resultPage);
  }

  public ResultVo save(Long userId, KbDatasetModel kbDatasetModel) {
    ResultVo resultVo = new ResultVo();
    TableResult<Kv> saveOrUpdate = Aop.get(MaxKbDatasetDao.class).saveOrUpdate(userId, kbDatasetModel);
    return resultVo.setData(saveOrUpdate.getData());
  }

  public ResultVo get(Long userId, Long id) {
    ResultVo resultVo = new ResultVo();
    TableInput tableInput = new TableInput();
    if (userId.equals(1L)) {
      tableInput.set("id", id);
    } else {
      tableInput.set("id", id).set("user_id", userId);
    }
    TableResult<Record> result = ApiTable.get(TableNames.max_kb_dataset, tableInput);
    Kv kv = result.getData().toKv();
    return resultVo.setData(kv);
  }

  public ResultVo list(Long userId) {
    Record queryRecord = new Record();
    if (userId.equals(1L)) {

    } else {
      queryRecord.set("user_id", userId);
    }

    String columns = "id,name,\"desc\",type,meta,user_id,embedding_mode_id,create_time,update_time";
    List<Record> records = Db.find(MaxKbDataset.tableName, columns, queryRecord);
    List<Kv> kvs = RecordUtils.recordsToKv(records, false);
    return ResultVo.ok(kvs);
  }

  public ResultVo delete(Long userId, Long id) {
    TableInput tableInput = TableInput.by("id", id).set("user_id", userId);
    TableResult<Boolean> result = ApiTable.delete(TableNames.max_kb_dataset, tableInput);
    Record deleteRecord = Record.by("dataset_id", id);
    Db.delete(TableNames.max_kb_document, deleteRecord);
    Db.delete(TableNames.max_kb_paragraph, deleteRecord);
    return new ResultVo(result.getData());
  }

  public ResultVo getApplicationByDatasetId(Long datasetId) {
    return ResultVo.ok();
  }
}
