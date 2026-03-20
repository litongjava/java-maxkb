package com.litongjava.maxkb.controller;

import java.io.IOException;
import java.sql.SQLException;

import com.litongjava.annotation.EnableCORS;
import com.litongjava.annotation.RequestPath;
import com.litongjava.maxkb.model.MaxKbParagraph;
import com.litongjava.model.body.RespBodyVo;
import com.litongjava.tio.boot.admin.utils.ApiTableUtils;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;

@RequestPath("/api/table/max_kb_paragraph")
@EnableCORS
public class ApiTableMaxKbParagraphController {

  private final String from = MaxKbParagraph.tableName;

  @RequestPath("/create")
  public RespBodyVo create(HttpRequest request) {
    return ApiTableUtils.create(from, request);
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
    return ApiTableUtils.page(from, request);
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

}
