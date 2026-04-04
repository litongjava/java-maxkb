package com.litongjava.maxkb.controller;

import nexus.io.annotation.Get;
import nexus.io.annotation.RequestPath;
import nexus.io.model.result.ResultVo;

@RequestPath("/api/email_setting")
public class ApiEmailSettingController {

  @Get
  public ResultVo index() {
    return ResultVo.ok();
  }
}
