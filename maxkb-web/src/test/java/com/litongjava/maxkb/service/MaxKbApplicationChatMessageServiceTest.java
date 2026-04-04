package com.litongjava.maxkb.service;

import org.junit.Test;

import com.litongjava.maxkb.config.MaxKbDbConfig;
import com.litongjava.maxkb.service.kb.MaxKbApplicationChatMessageService;
import com.litongjava.maxkb.vo.MaxKbChatRequestVo;

import nexus.io.jfinal.aop.Aop;
import nexus.io.model.result.ResultVo;
import nexus.io.tio.boot.testing.TioBootTest;
import nexus.io.tio.utils.json.JsonUtils;

public class MaxKbApplicationChatMessageServiceTest {

  @Test
  public void test() {
    TioBootTest.runWith(MaxKbDbConfig.class);
    MaxKbChatRequestVo chatRequestVo = new MaxKbChatRequestVo();
    chatRequestVo.setMessage("office hour");
    ResultVo ask = Aop.get(MaxKbApplicationChatMessageService.class).ask(null, 446472660186722304L, chatRequestVo);
    System.out.println(JsonUtils.toJson(ask));
  }
}
