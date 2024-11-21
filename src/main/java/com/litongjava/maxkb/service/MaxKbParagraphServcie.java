package com.litongjava.maxkb.service;

import java.util.ArrayList;
import java.util.List;

import org.postgresql.util.PGobject;

import com.jfinal.kit.Kv;
import com.litongjava.db.TableInput;
import com.litongjava.db.TableResult;
import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Record;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.kit.RecordUtils;
import com.litongjava.maxkb.constant.TableNames;
import com.litongjava.maxkb.vo.Paragraph;
import com.litongjava.maxkb.vo.ResultPage;
import com.litongjava.model.page.Page;
import com.litongjava.model.result.ResultVo;
import com.litongjava.table.services.ApiTable;
import com.litongjava.tio.utils.crypto.Md5Utils;
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

  public ResultVo create(Long userId, Long datasetId, Long documentId, Paragraph p) {
    TableInput tableInput = new TableInput();
    tableInput.set("id", datasetId);
    if (!userId.equals(1L)) {
      tableInput.set("user_id", userId);
    }

    TableResult<Record> result = ApiTable.get(TableNames.max_kb_dataset, tableInput);

    Record dataset = result.getData();
    if (dataset == null) {
      return ResultVo.fail("Dataset not found.");
    }

    Long embedding_mode_id = dataset.getLong("embedding_mode_id");
    String sqlModelName = String.format("SELECT model_name FROM %s WHERE id = ?", TableNames.max_kb_model);
    String modelName = Db.queryStr(sqlModelName, embedding_mode_id);

    MaxKbEmbeddingService maxKbEmbeddingService = Aop.get(MaxKbEmbeddingService.class);

    String title = p.getTitle();
    String content = p.getContent();
    PGobject vector = maxKbEmbeddingService.getVector(content, modelName);
    Record record = Record.by("id", SnowflakeIdUtils.id())
        //
        //.set("source_id", )
        //
        .set("source_type", "md")
        //
        .set("title", title)
        //
        .set("content", content)
        //
        .set("md5", Md5Utils.getMD5(content))
        //
        .set("status", "1")
        //
        .set("hit_num", 0)
        //
        .set("is_active", true).set("dataset_id", datasetId).set("document_id", documentId)
        //
        .set("embedding", vector);

    Db.save(TableNames.max_kb_paragraph, record);

    return ResultVo.ok();
  }

}
