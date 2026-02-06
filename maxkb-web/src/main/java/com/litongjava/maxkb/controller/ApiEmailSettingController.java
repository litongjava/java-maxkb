package com.litongjava.maxkb.controller;

import com.litongjava.annotation.Get;
import com.litongjava.annotation.RequestPath;
import com.litongjava.model.result.ResultVo;

@RequestPath("/api/email_setting")
public class ApiEmailSettingController {

  @Get
  public ResultVo index() {
    return ResultVo.ok();
  }
}
