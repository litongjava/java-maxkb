package com.litongjava.maxkb.handler;

import com.litongjava.model.result.ResultVo;
import com.litongjava.tio.boot.exception.TioBootExceptionHandler;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.websocket.common.WebSocketRequest;

public class GlobalExceptionHandler implements TioBootExceptionHandler {

  @Override
  public ResultVo handler(HttpRequest request, Throwable e) {
    // 返回自定义的错误响应
    return ResultVo.fail(e.getMessage());
  }

  @Override
  public Object wsTextHandler(WebSocketRequest webSokcetRequest, String text, ChannelContext channelContext, HttpRequest httpRequest, Throwable e) {
    return null;
  }

  @Override
  public Object wsBytesHandler(WebSocketRequest webSokcetRequest, byte[] bytes, ChannelContext channelContext, HttpRequest httpRequest, Throwable e) {
    return null;
  }
}
