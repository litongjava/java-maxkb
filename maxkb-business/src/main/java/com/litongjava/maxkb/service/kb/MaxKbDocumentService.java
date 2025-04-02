package com.litongjava.maxkb.service.kb;

import java.util.List;

import com.jfinal.kit.Kv;
import com.litongjava.db.TableInput;
import com.litongjava.db.TableResult;
import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.kit.RowUtils;
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
    TableResult<Page<Row>> tableResult = ApiTable.page(TableNames.max_kb_document, tableInput);
    Page<Row> page = tableResult.getData();
    int totalRow = page.getTotalRow();
    List<Row> list = page.getList();
    List<Kv> kvs = RowUtils.toKv(list, false);
    ResultPage<Kv> resultPage = new ResultPage<>(pageNo, pageSize, totalRow, kvs);
    return ResultVo.ok(resultPage);
  }

  public ResultVo list(Long userId, Long datasetId) {
    TableInput tableInput = new TableInput();
    if (userId != 1) {
      tableInput.set("user_id", userId);
    }
    tableInput.set("dataset_id", datasetId);
    TableResult<List<Row>> tableResult = ApiTable.list(TableNames.max_kb_document, tableInput);
    List<Row> records = tableResult.getData();
    List<Kv> kvs = RowUtils.toKv(records, false);
    return ResultVo.ok(kvs);
  }

  public ResultVo get(Long userId, Long datasetId, Long documentId) {
    TableInput tableInput = TableInput.by("id", documentId);
    if (userId == 1) {
      tableInput.set("dataset_id", datasetId);
    } else {
      tableInput.set("user_id", userId).set("dataset_id", datasetId);
    }

    Row data = ApiTable.get(TableNames.max_kb_document, tableInput).getData();
    return ResultVo.ok(data.toKv());
  }

  public ResultVo delete(Long userId, Long datasetId, Long documentId) {
    Row record = Row.by("id", documentId).set("user_id", userId).set("dataset_id", datasetId);
    Db.delete(TableNames.max_kb_document, record);
    record = Row.by("document_id", documentId).set("dataset_id", datasetId);
    Db.delete(TableNames.max_kb_paragraph, record);
    return ResultVo.ok();
  }

}
