package com.litongjava.maxkb.handler;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.service.api.RagDatasetRetrievalService;
import com.litongjava.maxkb.vo.ApiRagDatasetResponse;
import com.litongjava.maxkb.vo.ApiRagDatasetRetrievalRequest;
import com.litongjava.model.result.ResultVo;
import com.litongjava.tio.boot.admin.utils.TioAdminEnvUtils;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.HttpResponseStatus;
import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.utils.json.JsonUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApiRagDatasetRetrievalTitleHandler {

  private RagDatasetRetrievalService ragDatasetRetrievalService = Aop.get(RagDatasetRetrievalService.class);

  public HttpResponse index(HttpRequest request) {
    HttpResponse response = TioRequestContext.getResponse();

    String authorization = request.getAuthorization();
    if (authorization == null) {
      response.setStatus(HttpResponseStatus.C401);
      ResultVo fail = ResultVo.fail("authorization is empty");
      response.setJson(fail);
      return response;
    }
    String[] split = authorization.split(" ");
    if (split.length < 1) {
      response.setStatus(HttpResponseStatus.C401);
      ResultVo fail = ResultVo.fail("authorization is empty");
      response.setJson(fail);
      return response;
    }
    String token = TioAdminEnvUtils.getAdminToken();
    if (!token.equals(split[1])) {
      ResultVo fail = ResultVo.fail("Failed to validate token");
      response.setJson(fail);
      return response;
    }

    // 1. 解析请求
    String bodyString = request.getBodyString();

    if (StrUtil.isBlank(bodyString)) {
      response.setStatus(HttpResponseStatus.C400);
      ResultVo fail = ResultVo.fail("request body is empty");
      response.setJson(fail);
      return response;
    }

    ApiRagDatasetRetrievalRequest req = JsonUtils.parse(bodyString, ApiRagDatasetRetrievalRequest.class);

    // 2. 基础参数校验
    if (req.getDatasetName() == null || req.getDatasetName().isEmpty() || req.getInput() == null
        || req.getInput().isEmpty()) {

      response.setStatus(HttpResponseStatus.C400);
      ResultVo json = ResultVo.fail("datasetName and input are required");
      response.setJson(json);
      return response;
    }

    try {

      // 3. 返回JSON
      ApiRagDatasetResponse resp = ragDatasetRetrievalService.retrievalByTitle(req);
      response.setJson(JsonUtils.toJson(resp));
      return response;
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setStatus(HttpResponseStatus.C500);
      ResultVo fail = ResultVo.fail("internal server error");
      response.setJson(fail);
      return response;
    }
  }
}
