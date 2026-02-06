package com.litongjava.maxkb.controller;

import com.litongjava.annotation.RequestPath;
import com.litongjava.model.result.ResultVo;

@RequestPath("/api/display")
public class ApiDisplayController {

  public ResultVo info() {
    return ResultVo.ok();
  }
}
