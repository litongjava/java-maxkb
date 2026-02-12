package com.litongjava.maxkb.controller;

import com.litongjava.annotation.Get;
import com.litongjava.annotation.RequestPath;
import com.litongjava.model.result.ResultVo;

@RequestPath("/api/qr_type")
public class ApiQrTypeController {

  @Get
  public ResultVo index() {
    //{ "code": 200, "data": ["wecom", "dingtalk"] }
    String[] data = { "wecom", "dingtalk" };
    return ResultVo.ok(data);
  }
}
