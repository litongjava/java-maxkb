package com.litongjava.maxkb.handler;

import com.litongjava.model.result.ResultVo;
import com.litongjava.tio.boot.exception.TioBootExceptionHandler;
import com.litongjava.tio.http.common.HttpRequest;

public class GlobalExceptionHandler implements TioBootExceptionHandler {

  @Override
  public ResultVo handler(HttpRequest request, Throwable e) {
    // 返回自定义的错误响应
    return ResultVo.fail(e.getMessage());
  }
}
