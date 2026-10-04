package com.litongjava.mosskb.service;

import org.junit.Test;

import nexus.io.jfinal.aop.Aop;
import nexus.io.mosskb.config.MossKbDbConfig;
import nexus.io.mosskb.service.kb.MossKbParagraphRetrieveService;
import nexus.io.mosskb.vo.MossKbRetrieveResult;
import nexus.io.tio.boot.testing.TioBootTest;
import nexus.io.tio.utils.json.JsonUtils;

public class MossKbParagraphSearchServiceTest {

  @Test
  public void test() {
    TioBootTest.runWith(MossKbDbConfig.class);
    Long[] datasetIdArray = { 474194391515324416L };
    Float similarity = 0.2f;
    Integer top_n = 20;
    String question = "总结一下文档内容";
    MossKbRetrieveResult search = Aop.get(MossKbParagraphRetrieveService.class).retrieve(datasetIdArray, similarity, top_n, question);
    System.out.println(JsonUtils.toJson(search.getParagraph_list()));
  }

}
