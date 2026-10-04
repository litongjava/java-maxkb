package com.litongjava.mosskb.service;

import org.junit.Test;

import nexus.io.jfinal.aop.Aop;
import nexus.io.mosskb.config.MossKbDbConfig;
import nexus.io.mosskb.service.kb.MossKbApplicationChatMessageService;
import nexus.io.mosskb.vo.MossKbChatRequestVo;
import nexus.io.model.result.ResultVo;
import nexus.io.tio.boot.testing.TioBootTest;
import nexus.io.tio.utils.json.JsonUtils;

public class MossKbApplicationChatMessageServiceTest {

  @Test
  public void test() {
    TioBootTest.runWith(MossKbDbConfig.class);
    MossKbChatRequestVo chatRequestVo = new MossKbChatRequestVo();
    chatRequestVo.setMessage("office hour");
    ResultVo ask = Aop.get(MossKbApplicationChatMessageService.class).ask(null, 446472660186722304L, chatRequestVo);
    System.out.println(JsonUtils.toJson(ask));
  }
}
