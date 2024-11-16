package com.litongjava.maxkb.service;

import org.junit.Test;

import com.litongjava.db.TableInput;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.config.DbConfig;
import com.litongjava.model.result.ResultVo;
import com.litongjava.tio.boot.testing.TioBootTest;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.json.JsonUtils;

public class MaxKbApplicationServiceTest {

  @Test
  public void testPage() {
    EnvUtils.load();
    new DbConfig().config();
    TableInput tableInput = new TableInput();
    tableInput.setPageNo(1).setPageSize(20);
    ResultVo page = Aop.get(MaxKbApplicationService.class).page(tableInput);
    System.out.println(JsonUtils.toJson(page));
  }
  
  @Test
  public void testGet() {
    TioBootTest.runWith(DbConfig.class);
    ResultVo resultVo = Aop.get(MaxKbApplicationService.class).get(1L, 445801809260630016L);
    System.out.println(JsonUtils.toJson(resultVo));
  }

  @Test
  public void getList() {
    TioBootTest.runWith(DbConfig.class);
    ResultVo resultVo = Aop.get(MaxKbApplicationService.class).list(1L);
    System.out.println(JsonUtils.toJson(resultVo));
  }

}
