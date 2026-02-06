package com.litongjava.maxkb.controller;

import com.litongjava.annotation.Get;
import com.litongjava.annotation.RequestPath;
import com.litongjava.model.result.ResultVo;

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
