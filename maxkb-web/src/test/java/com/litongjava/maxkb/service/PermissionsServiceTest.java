package com.litongjava.maxkb.service;

import java.util.List;

import org.junit.Test;

import nexus.io.jfinal.aop.Aop;
import nexus.io.maxkb.config.MaxKbDbConfig;
import nexus.io.maxkb.service.PermissionsService;
import nexus.io.tio.utils.environment.EnvUtils;
import nexus.io.tio.utils.json.JsonUtils;

public class PermissionsServiceTest {

  @Test
  public void test() {
    EnvUtils.load();
    new MaxKbDbConfig().config();
    List<String> permissions = Aop.get(PermissionsService.class).getPermissionsByRole("ADMIN");
    System.out.println(JsonUtils.toJson(permissions));
  }

}
