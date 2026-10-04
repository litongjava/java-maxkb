package nexus.io.mosskb.controller;

import nexus.io.annotation.RequestPath;
import nexus.io.jfinal.aop.Aop;
import nexus.io.mosskb.service.kb.MossKbApplicationChatService;
import nexus.io.mosskb.vo.MossKbApplicationVo;
import nexus.io.model.result.ResultVo;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.utils.json.JsonUtils;

@RequestPath("/api/application/chat")
public class ApiApplicationChatController {

  public ResultVo open(HttpRequest request) {
    String bodyString = request.getBodyString();
    MossKbApplicationVo vo = JsonUtils.parse(bodyString, MossKbApplicationVo.class);
    return Aop.get(MossKbApplicationChatService.class).open(bodyString, vo);
  }
}
