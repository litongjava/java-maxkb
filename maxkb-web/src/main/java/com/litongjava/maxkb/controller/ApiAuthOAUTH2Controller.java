package com.litongjava.maxkb.controller;

import com.litongjava.annotation.RequestPath;
import com.litongjava.model.result.ResultVo;

@RequestPath("/api/auth/OAUTH2")
public class ApiAuthOAUTH2Controller {

  public ResultVo detail() {
    return ResultVo.ok();
  }
}
