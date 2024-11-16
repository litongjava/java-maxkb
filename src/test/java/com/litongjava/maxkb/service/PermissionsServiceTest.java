package com.litongjava.maxkb.service;

import java.util.List;

import org.junit.Test;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.config.DbConfig;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.json.JsonUtils;

public class PermissionsServiceTest {

  @Test
  public void test() {
    EnvUtils.load();
    new DbConfig().config();
    List<String> permissions = Aop.get(PermissionsService.class).getPermissionsByRole("ADMIN");
    System.out.println(JsonUtils.toJson(permissions));
  }

}
