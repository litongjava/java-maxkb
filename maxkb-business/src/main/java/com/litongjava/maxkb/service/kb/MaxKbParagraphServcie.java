package com.litongjava.maxkb.service.kb;

import java.util.ArrayList;
import java.util.List;

import org.postgresql.util.PGobject;

import com.jfinal.kit.Kv;
import com.litongjava.db.TableInput;
import com.litongjava.db.TableResult;
import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.kit.RowUtils;
import com.litongjava.maxkb.constant.MaxKbTableNames;
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

    TableResult<Page<Row>> tableResult = ApiTable.page(MaxKbTableNames.max_kb_paragraph, tableInput);
    Page<Row> page = tableResult.getData();
    int totalRow = page.getTotalRow();
    List<Row> records = page.getList();
    List<Kv> kvs = RowUtils.toKv(records, false);
    ResultPage<Kv> resultPage = new ResultPage<>(pageNo, pageSize, totalRow, kvs);
    return ResultVo.ok(resultPage);
  }

  public ResultVo listProblemByParagraphId(Long datasetId, Long documentId, Long paragraphId) {
    String sql = "select p.id,p.content,p.dataset_id from max_kb_problem p JOIN max_kb_problem_paragraph_mapping mapping on mapping.problem_id=p.id where mapping.paragraph_id=?";
    List<Row> records = Db.find(sql, paragraphId);
    List<Kv> kvs = RowUtils.toKv(records, false);
    return ResultVo.ok(kvs);
  }

  public ResultVo addProblemsById(Long datasetId, Long documentId, Long paragraphId, List<String> problems) {
    List<Row> problemRecords = new ArrayList<>();
    List<Row> mappings = new ArrayList<>();
    for (String str : problems) {
      long problemId = SnowflakeIdUtils.id();
      problemRecords.add(Row.by("id", problemId).set("dataset_id", datasetId).set("hit_num", 0).set("content", str));
      long mappingId = SnowflakeIdUtils.id();
      mappings.add(Row.by("id", mappingId).set("dataset_id", datasetId).set("document_id", documentId)
          //
          .set("paragraph_id", paragraphId).set("problem_id", problemId));
    }

    Db.tx(() -> {
      Db.batchSave(MaxKbTableNames.max_kb_problem, problemRecords, 2000);
      Db.batchSave(MaxKbTableNames.max_kb_problem_paragraph_mapping, mappings, 2000);
      return true;
    });
    return null;
  }

  public ResultVo addProblemById(Long datasetId, Long documentId, Long paragraphId, String content) {
    long problemId = SnowflakeIdUtils.id();
    Row problem = Row.by("id", problemId).set("dataset_id", datasetId).set("hit_num", 0).set("content", content);
    long mappingId = SnowflakeIdUtils.id();
    Row mapping = Row.by("id", mappingId).set("dataset_id", datasetId).set("document_id", documentId)
        //
        .set("paragraph_id", paragraphId).set("problem_id", problemId);
    ;
    Db.tx(() -> {
      Db.save(MaxKbTableNames.max_kb_problem, problem);
      Db.save(MaxKbTableNames.max_kb_problem_paragraph_mapping, mapping);
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

    TableResult<Row> result = ApiTable.get(MaxKbTableNames.max_kb_dataset, tableInput);

    Row dataset = result.getData();
    if (dataset == null) {
      return ResultVo.fail("Dataset not found.");
    }

    Long embedding_mode_id = dataset.getLong("embedding_mode_id");
    String sqlModelName = String.format("SELECT model_name FROM %s WHERE id = ?", MaxKbTableNames.max_kb_model);
    String modelName = Db.queryStr(sqlModelName, embedding_mode_id);

    KbEmbeddingService maxKbEmbeddingService = Aop.get(KbEmbeddingService.class);

    String title = p.getTitle();
    String content = p.getContent();
    PGobject vector = maxKbEmbeddingService.getVector(content, modelName);
    Row record = Row.by("id", SnowflakeIdUtils.id())
        //
        //.set("source_id", )
        //
        .set("source_type", "md")
        //
        .set("title", title)
        //
        .set("content", content)
        //
        .set("md5", Md5Utils.md5Hex(content))
        //
        .set("status", "1")
        //
        .set("hit_num", 0)
        //
        .set("is_active", true).set("dataset_id", datasetId).set("document_id", documentId)
        //
        .set("embedding", vector);

    Db.save(MaxKbTableNames.max_kb_paragraph, record);

    return ResultVo.ok();
  }

}
