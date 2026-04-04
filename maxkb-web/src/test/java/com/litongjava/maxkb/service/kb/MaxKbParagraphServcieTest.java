package com.litongjava.maxkb.service.kb;

import org.junit.Test;

import nexus.io.jfinal.aop.Aop;
import nexus.io.maxkb.config.boot.MaxKbBootConfig;
import nexus.io.maxkb.service.kb.MaxKbParagraphServcie;
import nexus.io.maxkb.vo.Paragraph;
import nexus.io.model.result.ResultVo;
import nexus.io.tio.boot.testing.TioBootTest;
import nexus.io.tio.utils.json.JsonUtils;

public class MaxKbParagraphServcieTest {

  @Test
  public void create() {
    TioBootTest.runWith(new MaxKbBootConfig());
    Paragraph p = new Paragraph("question", "anser");
    ResultVo resultVo = Aop.get(MaxKbParagraphServcie.class).create(01L, 01L, 01L, p);
    System.out.println(JsonUtils.toJson(resultVo));
  }
}
