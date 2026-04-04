package com.litongjava.maxkb.controller;

import com.litongjava.maxkb.service.kb.MaxKbApplicationChatService;
import com.litongjava.maxkb.vo.MaxKbApplicationVo;
import com.litongjava.tio.utils.json.JsonUtils;

import nexus.io.annotation.RequestPath;
import nexus.io.jfinal.aop.Aop;
import nexus.io.model.result.ResultVo;
import nexus.io.tio.http.common.HttpRequest;

@RequestPath("/api/application/chat")
public class ApiApplicationChatController {

  public ResultVo open(HttpRequest request) {
    String bodyString = request.getBodyString();
    MaxKbApplicationVo vo = JsonUtils.parse(bodyString, MaxKbApplicationVo.class);
    return Aop.get(MaxKbApplicationChatService.class).open(bodyString, vo);
  }
}
