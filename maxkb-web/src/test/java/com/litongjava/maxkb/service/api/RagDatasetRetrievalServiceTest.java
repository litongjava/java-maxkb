package com.litongjava.maxkb.service.api;

import java.util.List;

import org.junit.Test;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.config.boot.MaxKbBootConfig;
import com.litongjava.maxkb.vo.ApiRagDatasetRetrievalRequest;
import com.litongjava.maxkb.vo.MaxKbRetrievalResult;
import com.litongjava.tio.boot.admin.utils.PrintlnUtils;
import com.litongjava.tio.boot.testing.TioBootTest;

public class RagDatasetRetrievalServiceTest {

  @Test
  public void test() {
    TioBootTest.runWith(new MaxKbBootConfig());
    ApiRagDatasetRetrievalRequest request=new ApiRagDatasetRetrievalRequest() ;
    request.setDataset_name("plan_scene").setInput("勾股定理").setSimilarity(0.5d).setTop_number(10);
    List<MaxKbRetrievalResult> results = Aop.get(RagDatasetRetrievalService.class).retrievalByTitle(request);
    PrintlnUtils.printJson(results);
  }

}
