package nexus.io.maxkb.controller;

import com.alibaba.fastjson2.JSONObject;

import nexus.io.annotation.Delete;
import nexus.io.annotation.Get;
import nexus.io.annotation.Post;
import nexus.io.annotation.Put;
import nexus.io.annotation.RequestPath;
import nexus.io.jfinal.aop.Aop;
import nexus.io.maxkb.service.kb.MaxKbApiKeyService;
import nexus.io.model.result.ResultVo;
import nexus.io.tio.boot.http.TioRequestContext;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.utils.hutool.StrUtil;
import nexus.io.tio.utils.json.FastJson2Utils;

/**
 * 应用 API 密钥。路径前缀与 {@code ApiApplicationController} 相同，靠更具体的路径段匹配，
 * 因此带 api_key 的请求不会落到应用分页接口上。
 */
@RequestPath("/api/application")
public class ApiApplicationApiKeyController {

  @Get("/{applicationId}/api_key")
  public ResultVo list(Long applicationId) {
    return Aop.get(MaxKbApiKeyService.class).listApplicationKeys(TioRequestContext.getUserIdLong(), applicationId);
  }

  @Post("/{applicationId}/api_key")
  public ResultVo create(Long applicationId) {
    return Aop.get(MaxKbApiKeyService.class).createApplicationKey(TioRequestContext.getUserIdLong(), applicationId);
  }

  @Put("/{applicationId}/api_key/{apiKeyId}")
  public ResultVo update(Long applicationId, Long apiKeyId, HttpRequest request) {
    return Aop.get(MaxKbApiKeyService.class).updateApplicationKey(TioRequestContext.getUserIdLong(), applicationId,
        apiKeyId, body(request));
  }

  @Delete("/{applicationId}/api_key/{apiKeyId}")
  public ResultVo delete(Long applicationId, Long apiKeyId) {
    return Aop.get(MaxKbApiKeyService.class).deleteApplicationKey(TioRequestContext.getUserIdLong(), applicationId,
        apiKeyId);
  }

  private JSONObject body(HttpRequest request) {
    String bodyString = request.getBodyString();
    return StrUtil.isBlank(bodyString) ? new JSONObject() : FastJson2Utils.parseObject(bodyString);
  }
}
