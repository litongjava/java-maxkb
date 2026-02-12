package com.litongjava.maxkb.controller;

import com.litongjava.annotation.Get;
import com.litongjava.annotation.RequestPath;
import com.litongjava.model.result.ResultVo;

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
