package nexus.io.maxkb.service.kb;

import java.util.List;

import com.jfinal.kit.Kv;

import lombok.extern.slf4j.Slf4j;
import nexus.io.db.TableInput;
import nexus.io.db.TableResult;
import nexus.io.db.activerecord.Db;
import nexus.io.db.activerecord.Row;
import nexus.io.kit.RowUtils;
import nexus.io.maxkb.constant.MaxKbTableNames;
import nexus.io.maxkb.vo.MaxKbUpdateDocumentRequestVo;
import nexus.io.maxkb.vo.ResultPage;
import nexus.io.model.page.Page;
import nexus.io.model.result.ResultVo;
import nexus.io.table.services.ApiTable;

@Slf4j
public class MaxKbDocumentService {

  public ResultVo page(Long userId, Long datasetId, Integer pageNo, Integer pageSize) {
    TableInput tableInput = new TableInput();
    if (userId != 1) {
      tableInput.set("user_id", userId);
    }
    tableInput.set("dataset_id", datasetId).setPageNo(pageNo).setPageSize(pageSize);
    TableResult<Page<Row>> tableResult = ApiTable.page(MaxKbTableNames.max_kb_document, tableInput);
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
    TableResult<List<Row>> tableResult = ApiTable.list(MaxKbTableNames.max_kb_document, tableInput);
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

    Row data = ApiTable.get(MaxKbTableNames.max_kb_document, tableInput).getData();
    return ResultVo.ok(data.toKv());
  }

  public ResultVo delete(Long userId, Long datasetId, Long documentId) {
    Row row = Row.by("id", documentId).set("user_id", userId).set("dataset_id", datasetId);
    Db.delete(MaxKbTableNames.max_kb_document, row);
    row = Row.by("document_id", documentId).set("dataset_id", datasetId);
    Db.delete(MaxKbTableNames.max_kb_paragraph, row);
    return ResultVo.ok();
  }

  public ResultVo update(Long userId, Long datasetId, Long documentId, MaxKbUpdateDocumentRequestVo vo) {
    log.info("update vo:{}", vo);
    Row row = Row.by("id", documentId).set("user_id", userId).set("dataset_id", datasetId).set("name", vo.getName());
    Db.update(MaxKbTableNames.max_kb_document, row);
    return ResultVo.ok();
  }

}
