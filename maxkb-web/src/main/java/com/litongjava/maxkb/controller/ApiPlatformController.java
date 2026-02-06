package com.litongjava.maxkb.controller;

import com.litongjava.annotation.RequestPath;
import com.litongjava.model.result.ResultVo;

@RequestPath("/api/platform")
public class ApiPlatformController {

  public ResultVo source() {
    return ResultVo.ok();
  }
}
