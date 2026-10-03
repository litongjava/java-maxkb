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
 * 账号 API 密钥。官方前端的列表与新增请求带结尾斜杠，而结尾斜杠对纯字面量路径不参与匹配，
 * 因此每个字面量方法都成对注册一次：一次不带斜杠，一次带斜杠。
 */
@RequestPath("/api/system/api_key")
public class ApiSystemApiKeyController {

  @Get("")
  public ResultVo list() {
    return listKeys();
  }

  @Get("/")
  public ResultVo listWithTrailingSlash() {
    return listKeys();
  }

  @Post("")
  public ResultVo create() {
    return createKey();
  }

  @Post("/")
  public ResultVo createWithTrailingSlash() {
    return createKey();
  }

  @Put("/{apiKeyId}")
  public ResultVo update(Long apiKeyId, HttpRequest request) {
    return Aop.get(MaxKbApiKeyService.class).updateUserKey(TioRequestContext.getUserIdLong(), apiKeyId, body(request));
  }

  @Delete("/{apiKeyId}")
  public ResultVo delete(Long apiKeyId) {
    return Aop.get(MaxKbApiKeyService.class).deleteUserKey(TioRequestContext.getUserIdLong(), apiKeyId);
  }

  private ResultVo listKeys() {
    return Aop.get(MaxKbApiKeyService.class).listUserKeys(TioRequestContext.getUserIdLong());
  }

  private ResultVo createKey() {
    return Aop.get(MaxKbApiKeyService.class).createUserKey(TioRequestContext.getUserIdLong());
  }

  private JSONObject body(HttpRequest request) {
    String bodyString = request.getBodyString();
    return StrUtil.isBlank(bodyString) ? new JSONObject() : FastJson2Utils.parseObject(bodyString);
  }
}
