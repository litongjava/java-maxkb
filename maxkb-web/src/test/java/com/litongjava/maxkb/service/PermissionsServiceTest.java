package com.litongjava.maxkb.service;

import java.util.List;

import org.junit.Test;

import com.litongjava.maxkb.config.MaxKbDbConfig;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.json.JsonUtils;

import nexus.io.jfinal.aop.Aop;

public class PermissionsServiceTest {

  @Test
  public void test() {
    EnvUtils.load();
    new MaxKbDbConfig().config();
    List<String> permissions = Aop.get(PermissionsService.class).getPermissionsByRole("ADMIN");
    System.out.println(JsonUtils.toJson(permissions));
  }

}
