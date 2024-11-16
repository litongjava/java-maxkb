package com.litongjava.maxkb.service;

import java.util.List;

import com.jfinal.kit.Kv;
import com.litongjava.db.TableInput;
import com.litongjava.db.TableResult;
import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Record;
import com.litongjava.kit.RecordUtils;
import com.litongjava.maxkb.constant.TableNames;
import com.litongjava.maxkb.vo.ResultPage;
import com.litongjava.model.page.Page;
import com.litongjava.model.result.ResultVo;
import com.litongjava.table.services.ApiTable;

public class MaxKbDocumentService {
  public ResultVo page(Long userId, Long datasetId, Integer pageNo, Integer pageSize) {
    TableInput tableInput = new TableInput();
    if (userId != 1) {
      tableInput.set("user_id", userId);
    }
    tableInput.set("dataset_id", datasetId).setPageNo(pageNo).setPageSize(pageSize);
    TableResult<Page<Record>> tableResult = ApiTable.page(TableNames.max_kb_document, tableInput);
    Page<Record> page = tableResult.getData();
    int totalRow = page.getTotalRow();
    List<Record> list = page.getList();
    List<Kv> kvs = RecordUtils.recordsToKv(list, false);
    ResultPage<Kv> resultPage = new ResultPage<>(pageNo, pageSize, totalRow, kvs);
    return ResultVo.ok(resultPage);
  }

  public ResultVo list(Long userId, Long datasetId) {
    TableInput tableInput = new TableInput();
    if (userId != 1) {
      tableInput.set("user_id", userId);
    }
    tableInput.set("dataset_id", datasetId);
    TableResult<List<Record>> tableResult = ApiTable.list(TableNames.max_kb_document, tableInput);
    List<Record> records = tableResult.getData();
    List<Kv> kvs = RecordUtils.recordsToKv(records, false);
    return ResultVo.ok(kvs);
  }

  public ResultVo get(Long userId, Long datasetId, Long documentId) {
    TableInput tableInput = TableInput.by("id", documentId);
    if (userId == 1) {
      tableInput.set("dataset_id", datasetId);
    } else {
      tableInput.set("user_id", userId).set("dataset_id", datasetId);
    }

    Record data = ApiTable.get(TableNames.max_kb_document, tableInput).getData();
    return ResultVo.ok(data.toKv());
  }

  public ResultVo delete(Long userId, Long datasetId, Long documentId) {
    Record record = Record.by("id", documentId).set("user_id", userId).set("dataset_id", datasetId);
    Db.delete(TableNames.max_kb_document, record);
    record = Record.by("document_id", documentId).set("dataset_id", datasetId);
    Db.delete(TableNames.max_kb_paragraph, record);
    return ResultVo.ok();
  }

}
