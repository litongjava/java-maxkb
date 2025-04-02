package com.litongjava.maxkb.service;

import org.junit.Test;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.config.DbConfig;
import com.litongjava.maxkb.service.kb.MaxKbParagraphRetrieveService;
import com.litongjava.maxkb.vo.MaxKbSearchStep;
import com.litongjava.tio.boot.testing.TioBootTest;
import com.litongjava.tio.utils.json.JsonUtils;

public class MaxKbParagraphSearchServiceTest {

  @Test
  public void test() {
    TioBootTest.runWith(DbConfig.class);
    Long[] datasetIdArray = { 474194391515324416L };
    Float similarity = 0.2f;
    Integer top_n = 20;
    String question = "总结一下文档内容";
    MaxKbSearchStep search = Aop.get(MaxKbParagraphRetrieveService.class).search(datasetIdArray, similarity, top_n, question);
    System.out.println(JsonUtils.toJson(search.getParagraph_list()));
  }

}
