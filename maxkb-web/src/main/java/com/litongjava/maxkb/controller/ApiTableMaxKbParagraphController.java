package com.litongjava.maxkb.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jfinal.kit.Kv;
import com.litongjava.annotation.EnableCORS;
import com.litongjava.annotation.RequestPath;
import com.litongjava.db.TableInput;
import com.litongjava.db.TableResult;
import com.litongjava.db.activerecord.Row;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.model.MaxKbParagraph;
import com.litongjava.maxkb.service.kb.MaxKbParagraphServcie;
import com.litongjava.maxkb.vo.KbParagraph;
import com.litongjava.maxkb.vo.Paragraph;
import com.litongjava.model.body.RespBodyVo;
import com.litongjava.model.page.DbPage;
import com.litongjava.model.page.Page;
import com.litongjava.model.result.ResultVo;
import com.litongjava.table.services.ApiTable;
import com.litongjava.table.utils.TableInputUtils;
import com.litongjava.table.utils.TableResultUtils;
import com.litongjava.tio.boot.admin.utils.ApiTableUtils;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.boot.utils.TioRequestParamUtils;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.utils.json.JsonUtils;

@RequestPath("/api/table/max_kb_paragraph")
@EnableCORS
public class ApiTableMaxKbParagraphController {
  private final Logger log = LoggerFactory.getLogger(this.getClass());
  private final String from = MaxKbParagraph.tableName;

  private MaxKbParagraphServcie maxKbParagraphServcie = Aop.get(MaxKbParagraphServcie.class);
  private Long datasetId = 01L;
  private Long documentId = 01L;

  @RequestPath("/create")
  public RespBodyVo create(HttpRequest request) {
    String bodyString = request.getBodyString();
    KbParagraph kbParagraph = JsonUtils.parse(bodyString, KbParagraph.class);
    Long userId = TioRequestContext.getUserIdLong();
    Long id = kbParagraph.getId();
    ResultVo resultVo = null;
    Paragraph paragraph = new Paragraph(kbParagraph.getTitle(), kbParagraph.getContent());
    if (id == null) {
      resultVo = maxKbParagraphServcie.create(userId, datasetId, documentId, paragraph);
    } else {
      resultVo = maxKbParagraphServcie.update(userId, datasetId, documentId, id, paragraph);
    }

    return toRespBodyVo(resultVo);
  }

  @RequestPath("/list")
  public RespBodyVo list(HttpRequest request) {
    return ApiTableUtils.list(from, request);
  }

  @RequestPath("/listAll")
  public RespBodyVo listAll() {
    return ApiTableUtils.listAll(from);
  }

  @RequestPath("/page")
  public RespBodyVo page(HttpRequest request) {
    Map<String, Object> map = TioRequestParamUtils.getRequestMap(request);
    map.remove("f");
    map.remove("keyword");
    Object current = map.remove("current");
    if (current != null) {
      // add support for ant design pro table
      map.put("pageNo", current);
    }

    ApiTable.transformType(from, map);

    TableInput kv = TableInputUtils.camelToUnderscore(map);
    kv.setColumns("id,title,content,create_time,update_time");

    log.info("tableName:{},kv:{}", from, kv);
    TableResult<Page<Row>> page = ApiTable.page(from, kv);

    TableResult<DbPage<Kv>> dbJsonBean = TableResultUtils.pageToDbPage(page, false);
    return RespBodyVo.ok(dbJsonBean.getData()).code(dbJsonBean.getCode()).msg(dbJsonBean.getMsg());
  }

  @RequestPath("/get")
  public RespBodyVo get(HttpRequest request) {
    return ApiTableUtils.get(from, request);
  }

  @RequestPath("/update")
  public RespBodyVo update(HttpRequest request) {
    return ApiTableUtils.update(from, request);

  }

  @RequestPath("/batchUpdate")
  public RespBodyVo batchUpdate(HttpRequest request) {
    return ApiTableUtils.batchUpdate(from, request);
  }

  @RequestPath("/remove/{id}")
  public RespBodyVo remove(String id) {
    return ApiTableUtils.remove(from, id);
  }

  @RequestPath("/delete/{id}")
  public RespBodyVo delete(String id) {
    return ApiTableUtils.delete(from, id);
  }

  @RequestPath("/total")
  public RespBodyVo total() {
    return ApiTableUtils.total(from);
  }

  /**
   * 导出当前数据
   */
  @RequestPath("/export-current")
  public HttpResponse exportCurrent(HttpRequest request) throws IOException {
    return ApiTableUtils.exportCurrent(from, request);
  }

  /**
   * 导出所有数据
   */
  @RequestPath("/export-all")
  public HttpResponse exportAllExcel(HttpRequest request) throws IOException, SQLException {
    return ApiTableUtils.exportAllExcel(from, request);
  }

  @RequestPath("/pageDeleted")
  public RespBodyVo pageDeleted(HttpRequest request) {
    return ApiTableUtils.pageDeleted(from, request);
  }

  @RequestPath("/recover")
  public RespBodyVo recover(String id) {
    return ApiTableUtils.recover(from, id);
  }

  @RequestPath("/names")
  public RespBodyVo tableNames() throws IOException {
    return ApiTableUtils.tableNames();
  }

  @RequestPath("/config")
  public RespBodyVo fConfig(String lang) {
    return ApiTableUtils.fConfig(from, lang);
  }

  @RequestPath("/columns")
  public RespBodyVo proTableColumns(String f) {
    return ApiTableUtils.proTableColumns(from);
  }

  private RespBodyVo toRespBodyVo(ResultVo resultVo) {
    int code = resultVo.getCode();
    if (code == 200) {
      return RespBodyVo.ok(resultVo.getData()).code(resultVo.getCode()).msg(resultVo.getMessage());
    } else {
      return RespBodyVo.fail(resultVo.getMessage()).code(resultVo.getCode()).data(resultVo.getData());
    }
  }
}
