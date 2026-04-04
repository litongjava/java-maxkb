package com.litongjava.maxkb.service.api;

import java.util.List;

import org.junit.Test;

import com.litongjava.tio.boot.admin.utils.PrintlnUtils;

import nexus.io.jfinal.aop.Aop;
import nexus.io.maxkb.config.boot.MaxKbBootConfig;
import nexus.io.maxkb.service.api.MaxKbRagDatasetRetrievalService;
import nexus.io.maxkb.vo.ApiRagDatasetRetrievalRequest;
import nexus.io.maxkb.vo.MaxKbRetrievalResult;
import nexus.io.tio.boot.testing.TioBootTest;

public class RagDatasetRetrievalServiceTest {

  @Test
  public void test() {
    TioBootTest.runWith(new MaxKbBootConfig());
    ApiRagDatasetRetrievalRequest request=new ApiRagDatasetRetrievalRequest() ;
    request.setDataset_name("plan_scene").setInput("勾股定理").setSimilarity(0.5d).setTop_number(10);
    List<MaxKbRetrievalResult> results = Aop.get(MaxKbRagDatasetRetrievalService.class).retrievalByTitle(request);
    PrintlnUtils.printJson(results);
  }

}
