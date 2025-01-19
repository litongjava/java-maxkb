package com.litongjava.maxkb.service;

import java.util.ArrayList;
import java.util.List;

import com.jfinal.kit.Kv;
import com.litongjava.db.TableInput;
import com.litongjava.db.TableResult;
import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.kit.RowUtils;
import com.litongjava.maxkb.constant.TableNames;
import com.litongjava.maxkb.vo.MaxKbParagraphId;
import com.litongjava.maxkb.vo.ProbrolemCreateBatch;
import com.litongjava.maxkb.vo.ResultPage;
import com.litongjava.model.page.Page;
import com.litongjava.model.result.ResultVo;
import com.litongjava.table.services.ApiTable;
import com.litongjava.tio.utils.snowflake.SnowflakeIdUtils;

public class MaxKbProblemService {

  public ResultVo create(Long datasetId, List<String> problems) {
    List<Long> ids = new ArrayList<>();

    List<Row> records = new ArrayList<>();
    for (String string : problems) {
      long id = SnowflakeIdUtils.id();
      records.add(Row.by("id", id).set("content", string).set("dataset_id", datasetId).set("hit_num", 0));
      ids.add(id);
    }
    Db.batchSave(TableNames.max_kb_problem, records, 2000);
    return ResultVo.ok(ids);
  }

  public ResultVo page(Long datasetId, Integer pageNo, Integer pageSize) {
    TableInput tableInput = TableInput.create().setPageNo(pageNo).setPageSize(pageSize);
    TableResult<Page<Row>> page = ApiTable.page(TableNames.max_kb_problem, tableInput);
    int totalRow = page.getData().getTotalRow();
    List<Row> list = page.getData().getList();
    List<Kv> kvs = RowUtils.recordsToKv(list, false);
    ResultPage<Kv> resultPage = new ResultPage<>(pageNo, pageSize, totalRow, kvs);
    return ResultVo.ok(resultPage);
  }

  public ResultVo delete(Long datasetId, List<Long> ids) {
    ApiTable.deleteByIds(TableNames.max_kb_problem, ids);
    ApiTable.deleteByIds(TableNames.max_kb_problem_paragraph_mapping, "problem_id", ids);
    return ResultVo.ok();
  }

  public ResultVo delete(Long datasetId, Long problemId) {
    ApiTable.delById(TableNames.max_kb_problem, problemId);
    ApiTable.delById(TableNames.max_kb_problem_paragraph_mapping, "problem_id", problemId);
    return ResultVo.ok();
  }

  public ResultVo listParagraphByProblemId(Long datasetId, Long probleamId) {
    List<Kv> datas = new ArrayList<>();
    TableInput tableInput = TableInput.by("dataset_id", datasetId).set("problem_id", probleamId).columns("paragraph_id as id");
    TableResult<List<Row>> tableResult = ApiTable.list(TableNames.max_kb_problem_paragraph_mapping, tableInput);
    List<Row> records = tableResult.getData();
    if (records != null) {
      datas = RowUtils.recordsToKv(records, false);
    }
    return ResultVo.ok(datas);
  }

  public ResultVo association(Long datasetId, Long documentId, Long paragraphId, Long problemId) {
    long id = SnowflakeIdUtils.id();
    Row record = Row.by("id", id).set("dataset_id", datasetId).set("document_id", documentId)
        //
        .set("paragraph_id", paragraphId)
        //
        .set("problem_id", problemId);
    boolean save = Db.save(TableNames.max_kb_problem_paragraph_mapping, record);
    if (save) {
      return ResultVo.ok();
    } else {
      return ResultVo.fail();
    }
  }

  public ResultVo unAssociation(Long datasetId, Long documentId, Long paragraphId, Long problemId) {

    Row record = Row.by("dataset_id", datasetId).set("document_id", documentId)
        //
        .set("paragraph_id", paragraphId)
        //
        .set("problem_id", problemId);
    boolean ok = Db.delete(TableNames.max_kb_problem_paragraph_mapping, record);
    if (ok) {
      return ResultVo.ok();
    } else {
      return ResultVo.fail();
    }
  }

  public ResultVo addProbrolems(Long datasetId, ProbrolemCreateBatch batchReequest) {
    List<MaxKbParagraphId> paragraph_list = batchReequest.getParagraph_list();
    List<Long> problem_id_list = batchReequest.getProblem_id_list();

    List<Row> mappings = new ArrayList<>();
    for (int i = 0; i < paragraph_list.size(); i++) {
      MaxKbParagraphId maxKbParagraphId = paragraph_list.get(i);
      Long documentId = maxKbParagraphId.getDocument_id();
      Long paragraphId = maxKbParagraphId.getParagraph_id();

      for (Long problemId : problem_id_list) {
        long id = SnowflakeIdUtils.id();
        Row record = Row.by("id", id).set("dataset_id", datasetId).set("document_id", documentId)
            //
            .set("paragraph_id", paragraphId)
            //
            .set("problem_id", problemId);
        mappings.add(record);
      }

    }

    Db.batchSave(TableNames.max_kb_problem_paragraph_mapping, mappings, 2000);
    return ResultVo.ok();
  }

}
