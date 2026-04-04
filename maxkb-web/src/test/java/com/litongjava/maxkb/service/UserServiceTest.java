package com.litongjava.maxkb.service;

import org.junit.Test;

import nexus.io.jfinal.aop.Aop;
import nexus.io.maxkb.config.MaxKbDbConfig;
import nexus.io.maxkb.service.KbUserService;
import nexus.io.model.result.ResultVo;
import nexus.io.tio.utils.environment.EnvUtils;
import nexus.io.tio.utils.json.JsonUtils;

public class UserServiceTest {

  @Test
  public void test() {
    EnvUtils.load();
    new MaxKbDbConfig().config();
    ResultVo resultVo = Aop.get(KbUserService.class).index(1L);
    System.out.println(JsonUtils.toJson(resultVo));
  }

}
