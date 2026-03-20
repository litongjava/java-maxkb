package com.litongjava.maxkb.service.kb;

import org.junit.Test;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.config.boot.MaxKbBootConfig;
import com.litongjava.maxkb.vo.Paragraph;
import com.litongjava.model.result.ResultVo;
import com.litongjava.tio.boot.testing.TioBootTest;
import com.litongjava.tio.utils.json.JsonUtils;

public class MaxKbParagraphServcieTest {

  @Test
  public void create() {
    TioBootTest.runWith(new MaxKbBootConfig());
    Paragraph p = new Paragraph("question", "anser");
    ResultVo resultVo = Aop.get(MaxKbParagraphServcie.class).create(01L, 01L, 01L, p);
    System.out.println(JsonUtils.toJson(resultVo));
  }
}
