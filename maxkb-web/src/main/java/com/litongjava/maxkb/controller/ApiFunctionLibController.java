package com.litongjava.maxkb.controller;

import nexus.io.annotation.Get;
import nexus.io.annotation.RequestPath;
import nexus.io.model.result.ResultVo;

@RequestPath("/api/function_lib")
public class ApiFunctionLibController {


  @Get("/{pageNo}/{pageSize}")
  public ResultVo page(Integer pageNo, Integer pageSize) {
    return ResultVo.ok();
  }
  
  public ResultVo pylint() {
    return ResultVo.ok();
  }
  
}
