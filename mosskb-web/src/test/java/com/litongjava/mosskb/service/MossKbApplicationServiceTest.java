package com.litongjava.mosskb.service;

import org.junit.Test;

import nexus.io.db.TableInput;
import nexus.io.jfinal.aop.Aop;
import nexus.io.mosskb.config.MossKbDbConfig;
import nexus.io.mosskb.service.kb.MossKbApplicationService;
import nexus.io.model.result.ResultVo;
import nexus.io.tio.boot.testing.TioBootTest;
import nexus.io.tio.utils.environment.EnvUtils;
import nexus.io.tio.utils.json.JsonUtils;

public class MossKbApplicationServiceTest {

  @Test
  public void testPage() {
    EnvUtils.load();
    new MossKbDbConfig().config();
    TableInput tableInput = new TableInput();
    tableInput.setPageNo(1).setPageSize(20);
    ResultVo page = Aop.get(MossKbApplicationService.class).page(tableInput);
    System.out.println(JsonUtils.toJson(page));
  }
  
  @Test
  public void testGet() {
    TioBootTest.runWith(MossKbDbConfig.class);
    ResultVo resultVo = Aop.get(MossKbApplicationService.class).get(1L, 445801809260630016L);
    System.out.println(JsonUtils.toJson(resultVo));
  }

  @Test
  public void getList() {
    TioBootTest.runWith(MossKbDbConfig.class);
    ResultVo resultVo = Aop.get(MossKbApplicationService.class).list(1L);
    System.out.println(JsonUtils.toJson(resultVo));
  }

}
