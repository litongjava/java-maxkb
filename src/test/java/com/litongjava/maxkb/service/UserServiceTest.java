package com.litongjava.maxkb.service;

import org.junit.Test;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.config.DbConfig;
import com.litongjava.model.result.ResultVo;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.json.JsonUtils;

public class UserServiceTest {

  @Test
  public void test() {
    EnvUtils.load();
    new DbConfig().config();
    ResultVo resultVo = Aop.get(UserService.class).index(1L);
    System.out.println(JsonUtils.toJson(resultVo));
  }

}
