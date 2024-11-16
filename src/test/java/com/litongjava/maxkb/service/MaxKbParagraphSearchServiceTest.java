package com.litongjava.maxkb.service;

import org.junit.Test;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.config.DbConfig;
import com.litongjava.maxkb.vo.MaxKbSearchStep;
import com.litongjava.tio.boot.testing.TioBootTest;
import com.litongjava.tio.utils.json.JsonUtils;

public class MaxKbParagraphSearchServiceTest {

  @Test
  public void test() {
    TioBootTest.runWith(DbConfig.class);
    Long[] datasetIdArray = { 446225135519784960L };
    Float similarity = 0.2f;
    Integer top_n = 3;
    String question = "office hour";
    MaxKbSearchStep search = Aop.get(MaxKbParagraphSearchService.class).search(datasetIdArray, similarity, top_n, question);
    System.out.println(JsonUtils.toJson(search.getParagraph_list()));
  }

}
