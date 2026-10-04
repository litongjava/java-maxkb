package nexus.io.mosskb.controller;

import java.util.List;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import nexus.io.annotation.Delete;
import nexus.io.annotation.Get;
import nexus.io.annotation.Post;
import nexus.io.annotation.Put;
import nexus.io.annotation.RequestPath;
import nexus.io.jfinal.aop.Aop;
import nexus.io.mosskb.service.kb.MossKbDatasetHitTestService;
import nexus.io.mosskb.service.kb.MossKbDatasetService;
import nexus.io.mosskb.service.kb.MossKbDocumentService;
import nexus.io.mosskb.service.kb.MossKbParagraphServcie;
import nexus.io.mosskb.service.kb.MossKbParagraphSplitService;
import nexus.io.mosskb.service.kb.MossKbProblemService;
import nexus.io.mosskb.service.kb.MossKbWebDatasetService;
import nexus.io.mosskb.vo.KbDatasetModel;
import nexus.io.mosskb.vo.MossKbUpdateDocumentRequestVo;
import nexus.io.mosskb.vo.Paragraph;
import nexus.io.mosskb.vo.ParagraphBatchVo;
import nexus.io.mosskb.vo.ProbrolemCreateBatch;
import nexus.io.model.result.ResultVo;
import nexus.io.tio.boot.http.TioRequestContext;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.utils.json.JsonUtils;

@RequestPath("/api/dataset")
public class ApiDatasetController {

  @Get("")
  public ResultVo list() {
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(MossKbDatasetService.class).list(userId);
  }

  @Get("/{pageNo}/{pageSize}")
  public ResultVo page(Integer pageNo, Integer pageSize, String name) {
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(MossKbDatasetService.class).page(userId, pageNo, pageSize, name);
  }

  @Post("")
  public ResultVo save(HttpRequest request) {
    String bodyString = request.getBodyString();
    KbDatasetModel kbDatasetModel = JsonUtils.parse(bodyString, KbDatasetModel.class);
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(MossKbDatasetService.class).save(userId, kbDatasetModel);
  }

  /** 创建 Web 站点知识库：保存根地址与选择器后由后台线程抓取页面。 */
  @Post("/web")
  public ResultVo saveWebDataset(HttpRequest request) {
    JSONObject input = JSON.parseObject(request.getBodyString());
    return Aop.get(MossKbWebDatasetService.class).save(TioRequestContext.getUserIdLong(), input);
  }

  /** 同步 Web 站点知识库，sync_type 取 replace 或 complete。 */
  @Put("/{id}/sync_web")
  public ResultVo syncWebDataset(Long id, HttpRequest request) {
    return Aop.get(MossKbWebDatasetService.class).sync(TioRequestContext.getUserIdLong(), id,
        request.getParam("sync_type"));
  }

  /** 按地址列表向 Web 站点知识库导入网页文档。 */
  @Post("/{datasetId}/document/web")
  public ResultVo importWebDocument(Long datasetId, HttpRequest request) {
    JSONObject input = JSON.parseObject(request.getBodyString());
    return Aop.get(MossKbWebDatasetService.class).importDocuments(TioRequestContext.getUserIdLong(), datasetId, input);
  }

  /** 重新抓取一个网页文档。 */
  @Put("/{datasetId}/document/{documentId}/sync")
  public ResultVo syncWebDocument(Long datasetId, Long documentId) {
    return Aop.get(MossKbWebDatasetService.class).syncDocument(TioRequestContext.getUserIdLong(), datasetId, documentId);
  }

  /** 批量同步网页文档，请求体为 {"id_list":[...]}。 */
  @Put("/{datasetId}/document/_bach")
  public ResultVo batchSyncWebDocument(Long datasetId, HttpRequest request) {
    JSONObject input = JSON.parseObject(request.getBodyString());
    return Aop.get(MossKbWebDatasetService.class).batchSyncDocuments(TioRequestContext.getUserIdLong(), datasetId,
        input == null ? null : input.getJSONArray("id_list"));
  }

  @Get("/{id}")
  public ResultVo get(Long id) {
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(MossKbDatasetService.class).get(userId, id);
  }

  @Put("/{id}")
  public ResultVo updateDataset(Long id, HttpRequest request) {
    KbDatasetModel model = JsonUtils.parse(request.getBodyString(), KbDatasetModel.class);
    model.setId(id);
    return Aop.get(MossKbDatasetService.class).save(TioRequestContext.getUserIdLong(), model);
  }

  @Put("/{datasetId}/document/{documentId}/paragraph/{paragraphId}")
  public ResultVo updateParagraph(Long datasetId, Long documentId, Long paragraphId, HttpRequest request) {
    return Aop.get(MossKbParagraphServcie.class).update(TioRequestContext.getUserIdLong(), datasetId, documentId, paragraphId,
        JsonUtils.parse(request.getBodyString(), Paragraph.class));
  }

  @Delete("/{datasetId}/document/{documentId}/paragraph/{paragraphId}")
  public ResultVo deleteParagraph(Long datasetId, Long documentId, Long paragraphId) {
    return Aop.get(MossKbParagraphServcie.class).delete(TioRequestContext.getUserIdLong(), datasetId, documentId, paragraphId);
  }

  @Delete("/{id}")
  public ResultVo delete(Long id) {
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(MossKbDatasetService.class).delete(userId, id);
  }

  @Get("/{id}/hit_test")
  public ResultVo hitTest(Long id, HttpRequest request) {
    String query_text = request.getParam("query_text");
    Double similarity = request.getDouble("similarity");
    Integer top_number = request.getInt("top_number");
    String search_mode = request.getParam("search_mode");
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(MossKbDatasetHitTestService.class).hitTest(userId, id, query_text, similarity, top_number,
        search_mode);
  }

  @Post("/{id}/document/_bach")
  public ResultVo batch(Long id, HttpRequest request) {
    String bodyString = request.getBodyString();
    Long userId = TioRequestContext.getUserIdLong();
    List<ParagraphBatchVo> list = JsonUtils.parseArray(bodyString, ParagraphBatchVo.class);
    return Aop.get(MossKbParagraphSplitService.class).batch(userId, id, list);
  }

  @Get("/{id}/document/{pageNo}/{pageSize}")
  public ResultVo pageDocument(Long id, Integer pageNo, Integer pageSize) {
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(MossKbDocumentService.class).page(userId, id, pageNo, pageSize);
  }

  @Get("/{datasetId}/document")
  public ResultVo documentList(Long datasetId) {
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(MossKbDocumentService.class).list(userId, datasetId);
  }

  @Get("/{datasetId}/document/{documentId}")
  public ResultVo getDocument(Long datasetId, Long documentId) {
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(MossKbDocumentService.class).get(userId, datasetId, documentId);
  }

  @Delete("/{datasetId}/document/{documentId}")
  public ResultVo deleteDocument(Long datasetId, Long documentId) {
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(MossKbDocumentService.class).delete(userId, datasetId, documentId);
  }

  @Put("/{datasetId}/document/{documentId}")
  public ResultVo updateDocument(Long datasetId, Long documentId, HttpRequest httpRequest) {
    Long userId = TioRequestContext.getUserIdLong();
    String bodyString = httpRequest.getBodyString();
    if (bodyString == null) {
      return ResultVo.fail("request body can not be empty");
    }
    MossKbUpdateDocumentRequestVo vo = JsonUtils.parse(bodyString, MossKbUpdateDocumentRequestVo.class);
    return Aop.get(MossKbDocumentService.class).update(userId, datasetId, documentId, vo);
  }

  @Get("/{datasetId}/application")
  public ResultVo getApplicationByDatasetId(Long datasetId) {
    return Aop.get(MossKbDatasetService.class).getApplicationByDatasetId(datasetId);
  }

  @Post("/{datasetId}/document/{documentId}/paragraph")
  public ResultVo createParagraph(Long datasetId, Long documentId, HttpRequest request) {
    String bodyString = request.getBodyString();
    Paragraph paragraph = JsonUtils.parse(bodyString, Paragraph.class);
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(MossKbParagraphServcie.class).create(userId, datasetId, documentId, paragraph);
  }

  // paragraph
  @Get("/{datasetId}/document/{documentId}/paragraph/{pageNo}/{pageSize}")
  public ResultVo listParagraph(Long datasetId, Long documentId, Integer pageNo, Integer pageSize) {
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(MossKbParagraphServcie.class).page(userId, datasetId, documentId, pageNo, pageSize);
  }

  @Get("/{datasetId}/document/{documentId}/paragraph/{paragraphId}/problem")
  public ResultVo listProblem(Long datasetId, Long documentId, Long paragraphId) {
    return Aop.get(MossKbParagraphServcie.class).listProblemByParagraphId(datasetId, documentId, paragraphId);
  }

  @Post("/{datasetId}/document/{documentId}/paragraph/{paragraphId}/problem")
  public ResultVo addProblems(Long datasetId, Long documentId, Long paragraphId, HttpRequest request) {
    String bodyString = request.getBodyString();
    String content = JsonUtils.parseToMap(bodyString, String.class, String.class).get("content");
    return Aop.get(MossKbParagraphServcie.class).addProblemById(datasetId, documentId, paragraphId, content);
  }

  @Post("/{datasetId}/problem")
  public ResultVo createProblem(Long datasetId, HttpRequest request) {
    String bodyString = request.getBodyString();
    List<String> problems = JsonUtils.parseArray(bodyString, String.class);
    return Aop.get(MossKbProblemService.class).create(datasetId, problems);
  }


  @Get("/{datasetId}/problem/{pageNo}/{pageSize}")
  public ResultVo pageDatasetProbleam(Long datasetId, Integer pageNo, Integer pageSize) {
    return Aop.get(MossKbProblemService.class).page(datasetId, pageNo, pageSize);
  }

  // api/dataset/443309276048408576/problem/444734959665311744/paragraph
  @Get("/{datasetId}/problem/{problemId}/paragraph")
  public ResultVo listParagraphByProblemId(Long datasetId, Long problemId) {
    return Aop.get(MossKbProblemService.class).listParagraphByProblemId(datasetId, problemId);
  }

  @Put("/{datasetId}/document/{documentId}/paragraph/{paragraphId}/problem/{problemId}/association")
  public ResultVo association(Long datasetId, Long documentId, Long paragraphId, Long problemId) {
    return Aop.get(MossKbProblemService.class).association(datasetId, documentId, paragraphId, problemId);
  }

  @Put("/{datasetId}/document/{documentId}/paragraph/{paragraphId}/problem/{problemId}/un_association")
  public ResultVo unAssociation(Long datasetId, Long documentId, Long paragraphId, Long problemId) {
    return Aop.get(MossKbProblemService.class).unAssociation(datasetId, documentId, paragraphId, problemId);
  }

  @Post("/{datasetId}/problem/_batch")
  public ResultVo addProbrolems(Long datasetId, HttpRequest request) {
    String bodyString = request.getBodyString();
    ProbrolemCreateBatch batchReequest = JsonUtils.parse(bodyString, ProbrolemCreateBatch.class);
    return Aop.get(MossKbProblemService.class).addProbrolems(datasetId, batchReequest);
  }

  @Delete("/{datasetId}/problem/{problemId}")
  public ResultVo deleteProblem(Long datasetId, Long problemId) {
    return Aop.get(MossKbProblemService.class).delete(datasetId, problemId);
  }
  

  @Delete("/{datasetId}/problem/_batch")
  public ResultVo deleteProblemBatch(Long datasetId, HttpRequest request) {
    String bodyString = request.getBodyString();
    List<Long> problems = JsonUtils.parseArray(bodyString, Long.class);
    return Aop.get(MossKbProblemService.class).delete(datasetId, problems);
  }

}
