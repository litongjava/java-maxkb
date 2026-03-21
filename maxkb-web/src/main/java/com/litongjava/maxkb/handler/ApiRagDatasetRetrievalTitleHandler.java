package com.litongjava.maxkb.handler;

import java.util.List;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.service.api.RagDatasetRetrievalService;
import com.litongjava.maxkb.vo.ApiRagDatasetRetrievalRequest;
import com.litongjava.maxkb.vo.MaxKbRetrievalResult;
import com.litongjava.model.result.ResultVo;
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
    if (req.getDataset_name() == null || req.getDataset_name().isEmpty() || req.getInput() == null
        || req.getInput().isEmpty()) {
      response.setStatus(HttpResponseStatus.C400);
      ResultVo json = ResultVo.fail("datasetName and input are required");
      response.setJson(json);
      return response;
    }

    try {

      // 3. 返回JSON
      List<MaxKbRetrievalResult> results = ragDatasetRetrievalService.retrievalByTitle(req);
      ResultVo ok = ResultVo.ok(results);
      response.body(ok);
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
