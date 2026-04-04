package nexus.io.maxkb.handler;

import nexus.io.model.result.ResultVo;
import nexus.io.tio.boot.exception.TioBootExceptionHandler;
import nexus.io.tio.core.ChannelContext;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.websocket.common.WebSocketRequest;

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
