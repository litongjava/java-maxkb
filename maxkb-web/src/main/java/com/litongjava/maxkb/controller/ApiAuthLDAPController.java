package com.litongjava.maxkb.controller;

import com.litongjava.annotation.RequestPath;
import com.litongjava.model.result.ResultVo;

@RequestPath("/api/auth/LDAP")
public class ApiAuthLDAPController {

  public ResultVo detail() {
    return ResultVo.ok();
  }
}
