package com.litongjava.maxkb.handler;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.litongjava.maxkb.vo.ApiRagDatasetRequest;
import com.litongjava.maxkb.vo.ApiRagDatasetResponse;
import com.litongjava.maxkb.vo.KbParagraph;
import com.litongjava.model.result.ResultVo;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.HttpResponseStatus;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.json.JsonUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApiRagDatasetHandler {

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
    String token = EnvUtils.getStr("admin.token");
    if (!token.equals(split[1])) {
      ResultVo fail = ResultVo.fail("Failed to validate token");
      response.setJson(fail);
      return response;
    }

    try {
      // 1. 解析请求
      String bodyString = request.getBodyString();

      if (bodyString == null || bodyString.isEmpty()) {
        response.setStatus(HttpResponseStatus.C400);
        ResultVo fail = ResultVo.fail("request body is empty");
        response.setJson(fail);
        return response;
      }

      ApiRagDatasetRequest req = JsonUtils.parse(bodyString, ApiRagDatasetRequest.class);

      // 2. 基础参数校验
      if (req.getDatasetName() == null || req.getDatasetName().isEmpty() || req.getInput() == null
          || req.getInput().isEmpty()) {

        response.setStatus(HttpResponseStatus.C400);
        ResultVo json = ResultVo.fail("datasetName and input are required");
        response.setJson(json);
        return response;
      }

      List<KbParagraph> paragraphs = new ArrayList<>();

      Timestamp now = new Timestamp(System.currentTimeMillis());

      KbParagraph p1 = new KbParagraph(1L, "如何重置密码？", "请在登录页面点击【忘记密码】，通过邮箱验证码完成密码重置。", "1", 12, true, 100L, 200L, now,
          now);

      KbParagraph p2 = new KbParagraph(2L, "密码忘记怎么办？", "如果无法找回密码，请联系管理员或提交工单处理。", "1", 7, true, 100L, 201L, now, now);

      paragraphs.add(p1);
      paragraphs.add(p2);

      ApiRagDatasetResponse resp = new ApiRagDatasetResponse(paragraphs);

      // 3. 返回JSON
      response.setStatus(HttpResponseStatus.C200);
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
