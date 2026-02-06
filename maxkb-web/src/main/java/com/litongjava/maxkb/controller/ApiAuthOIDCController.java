package com.litongjava.maxkb.controller;

import com.litongjava.annotation.RequestPath;
import com.litongjava.model.result.ResultVo;

@RequestPath("/api/auth/OIDC")
public class ApiAuthOIDCController {

  public ResultVo detail() {
    return ResultVo.ok();
  }
}
