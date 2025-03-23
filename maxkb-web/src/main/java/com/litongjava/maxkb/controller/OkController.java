package com.litongjava.maxkb.controller;

import com.litongjava.annotation.RequestPath;
import com.litongjava.model.body.RespBodyVo;

@RequestPath("/ok")
public class OkController {

  @RequestPath
  public RespBodyVo ok() {
    return RespBodyVo.ok();
  }
}
