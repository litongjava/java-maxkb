package com.litongjava.maxkb.service;

import org.junit.Test;

import com.litongjava.maxkb.config.MaxKbDbConfig;
import com.litongjava.maxkb.service.kb.MaxKbApplicationService;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.json.JsonUtils;

import nexus.io.db.TableInput;
import nexus.io.jfinal.aop.Aop;
import nexus.io.model.result.ResultVo;
import nexus.io.tio.boot.testing.TioBootTest;

public class MaxKbApplicationServiceTest {

  @Test
  public void testPage() {
    EnvUtils.load();
    new MaxKbDbConfig().config();
    TableInput tableInput = new TableInput();
    tableInput.setPageNo(1).setPageSize(20);
    ResultVo page = Aop.get(MaxKbApplicationService.class).page(tableInput);
    System.out.println(JsonUtils.toJson(page));
  }
  
  @Test
  public void testGet() {
    TioBootTest.runWith(MaxKbDbConfig.class);
    ResultVo resultVo = Aop.get(MaxKbApplicationService.class).get(1L, 445801809260630016L);
    System.out.println(JsonUtils.toJson(resultVo));
  }

  @Test
  public void getList() {
    TioBootTest.runWith(MaxKbDbConfig.class);
    ResultVo resultVo = Aop.get(MaxKbApplicationService.class).list(1L);
    System.out.println(JsonUtils.toJson(resultVo));
  }

}
