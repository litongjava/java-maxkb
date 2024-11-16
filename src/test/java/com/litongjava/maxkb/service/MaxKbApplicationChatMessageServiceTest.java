package com.litongjava.maxkb.service;

import org.junit.Test;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.config.DbConfig;
import com.litongjava.maxkb.vo.MaxKbChatRequestVo;
import com.litongjava.model.result.ResultVo;
import com.litongjava.tio.boot.testing.TioBootTest;
import com.litongjava.tio.utils.json.JsonUtils;

public class MaxKbApplicationChatMessageServiceTest {

  @Test
  public void test() {
    TioBootTest.runWith(DbConfig.class);
    MaxKbChatRequestVo chatRequestVo = new MaxKbChatRequestVo();
    chatRequestVo.setMessage("office hour");
    ResultVo ask = Aop.get(MaxKbApplicationChatMessageService.class).ask(null, 446472660186722304L, chatRequestVo);
    System.out.println(JsonUtils.toJson(ask));
  }
}
