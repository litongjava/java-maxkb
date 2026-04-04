package com.litongjava.maxkb.controller;

import nexus.io.annotation.RequestPath;
import nexus.io.model.result.ResultVo;

@RequestPath("/api/auth/OAUTH2")
public class ApiAuthOAUTH2Controller {

  public ResultVo detail() {
    return ResultVo.ok();
  }
}
