package com.litongjava.maxkb.controller;

import com.litongjava.annotation.RequestPath;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.service.MaxKbApplicationChatService;
import com.litongjava.maxkb.vo.MaxKbApplicationVo;
import com.litongjava.model.result.ResultVo;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.utils.json.JsonUtils;

@RequestPath("/api/application/chat")
public class ApiApplicationChatController {

  public ResultVo open(HttpRequest request) {
    String bodyString = request.getBodyString();
    MaxKbApplicationVo vo = JsonUtils.parse(bodyString, MaxKbApplicationVo.class);
    return Aop.get(MaxKbApplicationChatService.class).open(bodyString, vo);
  }
}
