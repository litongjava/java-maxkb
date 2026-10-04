package com.litongjava.mosskb.service;

import org.junit.Test;

import nexus.io.jfinal.aop.Aop;
import nexus.io.mosskb.config.MossKbDbConfig;
import nexus.io.mosskb.service.KbUserService;
import nexus.io.model.result.ResultVo;
import nexus.io.tio.utils.environment.EnvUtils;
import nexus.io.tio.utils.json.JsonUtils;

public class UserServiceTest {

  @Test
  public void test() {
    EnvUtils.load();
    new MossKbDbConfig().config();
    ResultVo resultVo = Aop.get(KbUserService.class).index(1L);
    System.out.println(JsonUtils.toJson(resultVo));
  }

}
