package com.litongjava.maxkb.controller;

import nexus.io.annotation.RequestPath;
import nexus.io.model.result.ResultVo;

@RequestPath("/api/auth/CAS")
public class ApiAuthCASController {

  public ResultVo detail() {
    return ResultVo.ok();
  }
}
