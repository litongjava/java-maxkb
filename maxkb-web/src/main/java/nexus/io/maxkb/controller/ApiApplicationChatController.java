package nexus.io.maxkb.controller;

import nexus.io.annotation.RequestPath;
import nexus.io.jfinal.aop.Aop;
import nexus.io.maxkb.service.kb.MaxKbApplicationChatService;
import nexus.io.maxkb.vo.MaxKbApplicationVo;
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
