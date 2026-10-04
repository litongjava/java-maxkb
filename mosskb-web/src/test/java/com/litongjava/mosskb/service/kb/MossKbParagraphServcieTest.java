package com.litongjava.mosskb.service.kb;

import org.junit.Test;

import nexus.io.jfinal.aop.Aop;
import nexus.io.mosskb.config.boot.MossKbBootConfig;
import nexus.io.mosskb.service.kb.MossKbParagraphServcie;
import nexus.io.mosskb.vo.Paragraph;
import nexus.io.model.result.ResultVo;
import nexus.io.tio.boot.testing.TioBootTest;
import nexus.io.tio.utils.json.JsonUtils;

public class MossKbParagraphServcieTest {

  @Test
  public void create() {
    TioBootTest.runWith(new MossKbBootConfig());
    Paragraph p = new Paragraph("question", "anser");
    ResultVo resultVo = Aop.get(MossKbParagraphServcie.class).create(01L, 01L, 01L, p);
    System.out.println(JsonUtils.toJson(resultVo));
  }
}
