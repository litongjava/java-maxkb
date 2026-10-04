package com.litongjava.mosskb.service.api;

import java.util.List;

import org.junit.Assume;
import org.junit.Test;

import nexus.io.db.activerecord.Db;
import nexus.io.jfinal.aop.Aop;
import nexus.io.mosskb.config.boot.MossKbBootConfig;
import nexus.io.mosskb.service.api.MossKbRagDatasetRetrievalService;
import nexus.io.mosskb.vo.ApiRagDatasetRetrievalRequest;
import nexus.io.mosskb.vo.MossKbRetrievalResult;
import nexus.io.tio.boot.admin.utils.PrintlnUtils;
import nexus.io.tio.boot.testing.TioBootTest;

public class RagDatasetRetrievalServiceTest {

  private static final String DATASET_NAME = "plan_scene";

  @Test
  public void test() {
    TioBootTest.runWith(new MossKbBootConfig());
    // 该用例针对固定的业务知识库，库里没有这份数据时直接跳过。
    Integer datasets = Db.queryInt("select count(*) from moss_kb_dataset where name = ?", DATASET_NAME);
    Assume.assumeTrue("库里没有名为 " + DATASET_NAME + " 的知识库，跳过", datasets != null && datasets > 0);

    ApiRagDatasetRetrievalRequest request = new ApiRagDatasetRetrievalRequest();
    request.setDataset_name(DATASET_NAME).setInput("勾股定理").setSimilarity(0.5d).setTop_number(10);
    List<MossKbRetrievalResult> results = Aop.get(MossKbRagDatasetRetrievalService.class).retrievalByTitle(request);
    PrintlnUtils.printJson(results);
  }

}
