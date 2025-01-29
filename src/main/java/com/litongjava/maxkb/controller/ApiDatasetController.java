package com.litongjava.maxkb.controller;

import java.util.List;

import com.litongjava.annotation.Delete;
import com.litongjava.annotation.Get;
import com.litongjava.annotation.Post;
import com.litongjava.annotation.Put;
import com.litongjava.annotation.RequestPath;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.service.DatasetDocumentVectorService;
import com.litongjava.maxkb.service.kb.MaxKbDatasetHitTestService;
import com.litongjava.maxkb.service.kb.MaxKbDatasetService;
import com.litongjava.maxkb.service.kb.MaxKbDocumentService;
import com.litongjava.maxkb.service.kb.MaxKbParagraphServcie;
import com.litongjava.maxkb.service.kb.MaxKbProblemService;
import com.litongjava.maxkb.vo.DocumentBatchVo;
import com.litongjava.maxkb.vo.KbDatasetModel;
import com.litongjava.maxkb.vo.Paragraph;
import com.litongjava.maxkb.vo.ProbrolemCreateBatch;
import com.litongjava.model.result.ResultVo;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.utils.json.JsonUtils;

@RequestPath("/api/dataset")
public class ApiDatasetController {

  @Get("")
  public ResultVo list() {
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(MaxKbDatasetService.class).list(userId);
  }

  @Get("/{pageNo}/{pageSize}")
  public ResultVo page(Integer pageNo, Integer pageSize, String name) {
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(MaxKbDatasetService.class).page(userId, pageNo, pageSize, name);
  }

  @Post
  public ResultVo save(HttpRequest request) {
    String bodyString = request.getBodyString();
    KbDatasetModel kbDatasetModel = JsonUtils.parse(bodyString, KbDatasetModel.class);
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(MaxKbDatasetService.class).save(userId, kbDatasetModel);
  }

  @Get("/{id}")
  public ResultVo get(Long id) {
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(MaxKbDatasetService.class).get(userId, id);
  }

  @Delete("/{id}")
  public ResultVo delete(Long id) {
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(MaxKbDatasetService.class).delete(userId, id);
  }

  @Get("/{id}/hit_test")
  public ResultVo hitTest(Long id, HttpRequest request) {
    String query_text = request.getParam("query_text");
    Double similarity = request.getDouble("similarity");
    Integer top_number = request.getInt("top_number");
    String search_mode = request.getParam("search_mode");
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(MaxKbDatasetHitTestService.class).hitTest(userId, id, query_text, similarity, top_number, search_mode);
  }

  @Post("/{id}/document/_bach")
  public ResultVo batch(Long id, HttpRequest request) {
    String bodyString = request.getBodyString();
    Long userId = TioRequestContext.getUserIdLong();
    List<DocumentBatchVo> list = JsonUtils.parseArray(bodyString, DocumentBatchVo.class);
    return Aop.get(DatasetDocumentVectorService.class).batch(userId, id, list);
  }

  @Get("/{id}/document/{pageNo}/{pageSize}")
  public ResultVo pageDocument(Long id, Integer pageNo, Integer pageSize) {
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(MaxKbDocumentService.class).page(userId, id, pageNo, pageSize);
  }

  @Get("/{datasetId}/document")
  public ResultVo documentList(Long datasetId) {
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(MaxKbDocumentService.class).list(userId, datasetId);
  }

  @Get("/{datasetId}/document/{documentId}")
  public ResultVo getDocument(Long datasetId, Long documentId) {
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(MaxKbDocumentService.class).get(userId, datasetId, documentId);
  }

  @Delete("/{datasetId}/document/{documentId}")
  public ResultVo deleteDocument(Long datasetId, Long documentId) {
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(MaxKbDocumentService.class).delete(userId, datasetId, documentId);
  }

  @Get("/{datasetId}/application")
  public ResultVo getApplicationByDatasetId(Long datasetId) {
    return Aop.get(MaxKbDatasetService.class).getApplicationByDatasetId(datasetId);
  }

  @Post("/{datasetId}/document/{documentId}/paragraph")
  public ResultVo createParagraph(Long datasetId, Long documentId, HttpRequest request) {
    String bodyString = request.getBodyString();
    Paragraph paragraph = JsonUtils.parse(bodyString, Paragraph.class);
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(MaxKbParagraphServcie.class).create(userId, datasetId, documentId, paragraph);
  }

  //paragraph
  @Get("/{datasetId}/document/{documentId}/paragraph/{pageNo}/{pageSize}")
  public ResultVo listParagraph(Long datasetId, Long documentId, Integer pageNo, Integer pageSize) {
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(MaxKbParagraphServcie.class).page(userId, datasetId, documentId, pageNo, pageSize);
  }

  @Get("/{datasetId}/document/{documentId}/paragraph/{paragraphId}/problem")
  public ResultVo listProblem(Long datasetId, Long documentId, Long paragraphId) {
    return Aop.get(MaxKbParagraphServcie.class).listProblemByParagraphId(datasetId, documentId, paragraphId);
  }

  @Post("/{datasetId}/document/{documentId}/paragraph/{paragraphId}/problem")
  public ResultVo addProblems(Long datasetId, Long documentId, Long paragraphId, HttpRequest request) {
    String bodyString = request.getBodyString();
    String content = JsonUtils.parseToMap(bodyString, String.class, String.class).get("content");
    return Aop.get(MaxKbParagraphServcie.class).addProblemById(datasetId, documentId, paragraphId, content);
  }

  @Post("/{datasetId}/problem")
  public ResultVo createProblem(Long datasetId, HttpRequest request) {
    String bodyString = request.getBodyString();
    List<String> problems = JsonUtils.parseArray(bodyString, String.class);
    return Aop.get(MaxKbProblemService.class).create(datasetId, problems);
  }

  @Delete("/{datasetId}/problem/_batch")
  public ResultVo deleteProblem(Long datasetId, HttpRequest request) {
    String bodyString = request.getBodyString();
    List<Long> problems = JsonUtils.parseArray(bodyString, Long.class);
    return Aop.get(MaxKbProblemService.class).delete(datasetId, problems);
  }

  @Get("/{datasetId}/problem/{pageNo}/{pageSize}")
  public ResultVo pageDatasetProbleam(Long datasetId, Integer pageNo, Integer pageSize) {
    return Aop.get(MaxKbProblemService.class).page(datasetId, pageNo, pageSize);
  }

  //api/dataset/443309276048408576/problem/444734959665311744/paragraph
  @Get("/{datasetId}/problem/{problemId}/paragraph")
  public ResultVo listParagraphByProblemId(Long datasetId, Long problemId) {
    return Aop.get(MaxKbProblemService.class).listParagraphByProblemId(datasetId, problemId);
  }

  @Put("/{datasetId}/document/{documentId}/paragraph/{paragraphId}/problem/{problemId}/association")
  public ResultVo association(Long datasetId, Long documentId, Long paragraphId, Long problemId) {
    return Aop.get(MaxKbProblemService.class).association(datasetId, documentId, paragraphId, problemId);
  }

  @Put("/{datasetId}/document/{documentId}/paragraph/{paragraphId}/problem/{problemId}/un_association")
  public ResultVo unAssociation(Long datasetId, Long documentId, Long paragraphId, Long problemId) {
    return Aop.get(MaxKbProblemService.class).unAssociation(datasetId, documentId, paragraphId, problemId);
  }

  @Post("/{datasetId}/problem/_batch")
  public ResultVo addProbrolems(Long datasetId, HttpRequest request) {
    String bodyString = request.getBodyString();
    ProbrolemCreateBatch batchReequest = JsonUtils.parse(bodyString, ProbrolemCreateBatch.class);
    return Aop.get(MaxKbProblemService.class).addProbrolems(datasetId, batchReequest);
  }

  @Delete("/{datasetId}/problem/{problemId}")
  public ResultVo deleteProblem(Long datasetId, Long problemId) {
    return Aop.get(MaxKbProblemService.class).delete(datasetId, problemId);
  }

}
