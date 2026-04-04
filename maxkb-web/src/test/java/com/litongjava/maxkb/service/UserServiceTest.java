package com.litongjava.maxkb.service;

import org.junit.Test;

import com.litongjava.maxkb.config.MaxKbDbConfig;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.json.JsonUtils;

import nexus.io.jfinal.aop.Aop;
import nexus.io.model.result.ResultVo;

public class UserServiceTest {

  @Test
  public void test() {
    EnvUtils.load();
    new MaxKbDbConfig().config();
    ResultVo resultVo = Aop.get(KbUserService.class).index(1L);
    System.out.println(JsonUtils.toJson(resultVo));
  }

}
