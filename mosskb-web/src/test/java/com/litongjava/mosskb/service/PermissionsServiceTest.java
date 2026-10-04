package com.litongjava.mosskb.service;

import java.util.List;

import org.junit.Test;

import nexus.io.jfinal.aop.Aop;
import nexus.io.mosskb.config.MossKbDbConfig;
import nexus.io.mosskb.service.PermissionsService;
import nexus.io.tio.utils.environment.EnvUtils;
import nexus.io.tio.utils.json.JsonUtils;

public class PermissionsServiceTest {

  @Test
  public void test() {
    EnvUtils.load();
    new MossKbDbConfig().config();
    // 单参数重载要从 TioRequestContext 取当前用户，脱离 HTTP 请求会取到 null；
    // 这里直接走带 userId 的重载，用种子里的管理员账号。
    List<String> permissions = Aop.get(PermissionsService.class).getPermissionsByRole("ADMIN", 1L);
    System.out.println(JsonUtils.toJson(permissions));
  }

}
