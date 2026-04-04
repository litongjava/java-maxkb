package com.litongjava.maxkb.controller;

import com.litongjava.maxkb.service.kb.MaxKbApplicationChatService;
import com.litongjava.maxkb.vo.MaxKbApplicationVo;

import nexus.io.annotation.RequestPath;
import nexus.io.jfinal.aop.Aop;
import nexus.io.model.result.ResultVo;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.utils.json.JsonUtils;

@RequestPath("/api/application/chat")
public class ApiApplicationChatController {

  public ResultVo open(HttpRequest request) {
    String bodyString = request.getBodyString();
    MaxKbApplicationVo vo = JsonUtils.parse(bodyString, MaxKbApplicationVo.class);
    return Aop.get(MaxKbApplicationChatService.class).open(bodyString, vo);
  }
}
