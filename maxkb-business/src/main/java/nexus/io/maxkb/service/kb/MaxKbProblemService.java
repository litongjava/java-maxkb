package nexus.io.maxkb.service.kb;

import java.util.ArrayList;
import java.util.List;

import com.jfinal.kit.Kv;

import nexus.io.db.TableInput;
import nexus.io.db.TableResult;
import nexus.io.db.activerecord.Db;
import nexus.io.db.activerecord.Row;
import nexus.io.kit.RowUtils;
import nexus.io.maxkb.constant.MaxKbTableNames;
import nexus.io.maxkb.vo.MaxKbParagraphId;
import nexus.io.maxkb.vo.ProbrolemCreateBatch;
import nexus.io.maxkb.vo.ResultPage;
import nexus.io.model.page.Page;
import nexus.io.model.result.ResultVo;
import nexus.io.table.services.ApiTable;
import nexus.io.tio.utils.snowflake.SnowflakeIdUtils;

public class MaxKbProblemService {

  public ResultVo create(Long datasetId, List<String> problems) {
    List<Long> ids = new ArrayList<>();

    List<Row> records = new ArrayList<>();
    for (String string : problems) {
      long id = SnowflakeIdUtils.id();
      records.add(Row.by("id", id).set("content", string).set("dataset_id", datasetId).set("hit_num", 0));
      ids.add(id);
    }
    Db.batchSave(MaxKbTableNames.max_kb_problem, records, 2000);
    return ResultVo.ok(ids);
  }

  public ResultVo page(Long datasetId, Integer pageNo, Integer pageSize) {
    TableInput tableInput = TableInput.by("dataset_id", datasetId).setPageNo(pageNo).setPageSize(pageSize);
    TableResult<Page<Row>> page = ApiTable.page(MaxKbTableNames.max_kb_problem, tableInput);
    int totalRow = page.getData().getTotalRow();
    List<Row> list = page.getData().getList();
    List<Kv> kvs = RowUtils.toKv(list, false);
    ResultPage<Kv> resultPage = new ResultPage<>(pageNo, pageSize, totalRow, kvs);
    return ResultVo.ok(resultPage);
  }

  public ResultVo delete(Long datasetId, List<Long> ids) {
    for (Long id : ids) {
      if (Db.queryLong("select count(*) from max_kb_problem where id=? and dataset_id=?", id, datasetId) == 0) {
        return ResultVo.fail("问题不存在或不属于当前知识库");
      }
    }
    ApiTable.deleteByIds(MaxKbTableNames.max_kb_problem, ids);
    ApiTable.deleteByIds(MaxKbTableNames.max_kb_problem_paragraph_mapping, "problem_id", ids);
    return ResultVo.ok();
  }

  public ResultVo delete(Long datasetId, Long problemId) {
    if (Db.queryLong("select count(*) from max_kb_problem where id=? and dataset_id=?", problemId, datasetId) == 0) {
      return ResultVo.fail("问题不存在或不属于当前知识库");
    }
    ApiTable.delById(MaxKbTableNames.max_kb_problem, problemId);
    ApiTable.delById(MaxKbTableNames.max_kb_problem_paragraph_mapping, "problem_id", problemId);
    return ResultVo.ok();
  }

  public ResultVo listParagraphByProblemId(Long datasetId, Long probleamId) {
    List<Kv> datas = new ArrayList<>();
    TableInput tableInput = TableInput.by("dataset_id", datasetId).set("problem_id", probleamId).columns("paragraph_id as id");
    TableResult<List<Row>> tableResult = ApiTable.list(MaxKbTableNames.max_kb_problem_paragraph_mapping, tableInput);
    List<Row> records = tableResult.getData();
    if (records != null) {
      datas = RowUtils.toKv(records, false);
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
    boolean save = Db.save(MaxKbTableNames.max_kb_problem_paragraph_mapping, record);
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
    boolean ok = Db.delete(MaxKbTableNames.max_kb_problem_paragraph_mapping, record);
    if (ok) {
      return ResultVo.ok();
    } else {
      return ResultVo.fail();
    }
  }

  public ResultVo addProbrolems(Long datasetId, ProbrolemCreateBatch batchReequest) {
    List<MaxKbParagraphId> paragraph_list = batchReequest.getParagraph_list();
    List<Long> problem_id_list = batchReequest.getProblem_id_list();

    for (Long problemId : problem_id_list) {
      if (Db.queryLong("select count(*) from max_kb_problem where id=? and dataset_id=?", problemId, datasetId) == 0) {
        return ResultVo.fail("问题不属于当前知识库");
      }
    }
    for (MaxKbParagraphId paragraph : paragraph_list) {
      if (Db.queryLong("select count(*) from max_kb_paragraph where id=? and document_id=? and dataset_id=?", paragraph.getParagraph_id(), paragraph.getDocument_id(), datasetId) == 0) {
        return ResultVo.fail("段落不属于当前知识库");
      }
    }
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

    Db.batchSave(MaxKbTableNames.max_kb_problem_paragraph_mapping, mappings, 2000);
    return ResultVo.ok();
  }

}
