package com.litongjava.maxkb.controller;

import nexus.io.annotation.Get;
import nexus.io.annotation.RequestPath;
import nexus.io.model.result.ResultVo;

@RequestPath("/api/valid")
public class ApiValidController {

  /**
   * 社区版最多支持 50 个知识库,我这里设置为无限
   * @return
   */
  @Get("/dataset/{size}")
  public ResultVo dataset50(Integer size) {
    return ResultVo.ok(true);
  }
  
  @Get("/application/{size}")
  public ResultVo applicaiton5(Integer size) {
    return ResultVo.ok(true);
  }
}
