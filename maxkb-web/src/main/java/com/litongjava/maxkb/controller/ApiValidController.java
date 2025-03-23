package com.litongjava.maxkb.controller;

import com.litongjava.annotation.Get;
import com.litongjava.annotation.RequestPath;
import com.litongjava.model.result.ResultVo;

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
