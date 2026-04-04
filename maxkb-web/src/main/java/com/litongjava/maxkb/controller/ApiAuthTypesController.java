package com.litongjava.maxkb.controller;

import nexus.io.annotation.Get;
import nexus.io.annotation.RequestPath;
import nexus.io.model.result.ResultVo;

@RequestPath("/api/auth/types")
public class ApiAuthTypesController {

  @Get
  public ResultVo index() {
    // { "code": 200, "data": ["LDAP", "PASSWORD"] }
    //String[] data = { "LDAP", "PASSWORD" };
    String[] data = {"PASSWORD" };
    return ResultVo.ok(data);
  }
}
