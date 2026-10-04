package com.litongjava.mosskb.service.api;

import java.util.List;

import org.junit.Test;

import nexus.io.jfinal.aop.Aop;
import nexus.io.mosskb.config.boot.MossKbBootConfig;
import nexus.io.mosskb.service.api.MossKbRagDatasetRetrievalService;
import nexus.io.mosskb.vo.ApiRagDatasetRetrievalRequest;
import nexus.io.mosskb.vo.MossKbRetrievalResult;
import nexus.io.tio.boot.admin.utils.PrintlnUtils;
import nexus.io.tio.boot.testing.TioBootTest;

public class RagDatasetRetrievalServiceTest {

  @Test
  public void test() {
    TioBootTest.runWith(new MossKbBootConfig());
    ApiRagDatasetRetrievalRequest request=new ApiRagDatasetRetrievalRequest() ;
    request.setDataset_name("plan_scene").setInput("勾股定理").setSimilarity(0.5d).setTop_number(10);
    List<MossKbRetrievalResult> results = Aop.get(MossKbRagDatasetRetrievalService.class).retrievalByTitle(request);
    PrintlnUtils.printJson(results);
  }

}
