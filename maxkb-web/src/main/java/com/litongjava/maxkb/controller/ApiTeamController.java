package com.litongjava.maxkb.controller;

import com.litongjava.annotation.RequestPath;
import com.litongjava.model.result.ResultVo;

@RequestPath("/api/team")
public class ApiTeamController {

  @RequestPath("/member")
  public ResultVo member() {
    return ResultVo.ok();
  }
}
