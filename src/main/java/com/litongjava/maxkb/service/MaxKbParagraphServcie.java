package com.litongjava.maxkb.service;

import java.util.ArrayList;
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
import com.litongjava.tio.utils.snowflake.SnowflakeIdUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MaxKbParagraphServcie {

  public ResultVo page(Long userId, Long datasetId, Long documentId, Integer pageNo, Integer pageSize) {
    log.info("datasetId:{},documentId:{}", datasetId, documentId);
    TableInput tableInput = new TableInput();
    tableInput.setColumns("id,title,content,is_active,document_id,create_time,update_time");
    tableInput.set("dataset_id", datasetId).set("document_id", documentId);
    tableInput.setPageNo(pageNo).setPageSize(pageSize);

    TableResult<Page<Record>> tableResult = ApiTable.page(TableNames.max_kb_paragraph, tableInput);
    Page<Record> page = tableResult.getData();
    int totalRow = page.getTotalRow();
    List<Record> records = page.getList();
    List<Kv> kvs = RecordUtils.recordsToKv(records, false);
    ResultPage<Kv> resultPage = new ResultPage<>(pageNo, pageSize, totalRow, kvs);
    return ResultVo.ok(resultPage);
  }

  public ResultVo listProblemByParagraphId(Long datasetId, Long documentId, Long paragraphId) {
    String sql = "select p.id,p.content,p.dataset_id from max_kb_problem p JOIN max_kb_problem_paragraph_mapping mapping on mapping.problem_id=p.id where mapping.paragraph_id=?";
    List<Record> records = Db.find(sql, paragraphId);
    List<Kv> kvs = RecordUtils.recordsToKv(records, false);
    return ResultVo.ok(kvs);
  }

  public ResultVo addProblemsById(Long datasetId, Long documentId, Long paragraphId, List<String> problems) {
    List<Record> problemRecords = new ArrayList<>();
    List<Record> mappings = new ArrayList<>();
    for (String str : problems) {
      long problemId = SnowflakeIdUtils.id();
      problemRecords.add(Record.by("id", problemId).set("dataset_id", datasetId).set("hit_num", 0).set("content", str));
      long mappingId = SnowflakeIdUtils.id();
      mappings.add(Record.by("id", mappingId).set("dataset_id", datasetId).set("document_id", documentId)
          //
          .set("paragraph_id", paragraphId).set("problem_id", problemId));
    }

    Db.tx(() -> {
      Db.batchSave(TableNames.max_kb_problem, problemRecords, 2000);
      Db.batchSave(TableNames.max_kb_problem_paragraph_mapping, mappings, 2000);
      return true;
    });
    return null;
  }

  public ResultVo addProblemById(Long datasetId, Long documentId, Long paragraphId, String content) {
    long problemId = SnowflakeIdUtils.id();
    Record problem = Record.by("id", problemId).set("dataset_id", datasetId).set("hit_num", 0).set("content", content);
    long mappingId = SnowflakeIdUtils.id();
    Record mapping = Record.by("id", mappingId).set("dataset_id", datasetId).set("document_id", documentId)
        //
        .set("paragraph_id", paragraphId).set("problem_id", problemId);
    ;
    Db.tx(() -> {
      Db.save(TableNames.max_kb_problem, problem);
      Db.save(TableNames.max_kb_problem_paragraph_mapping, mapping);
      return true;
    });
    return null;
  }

}
