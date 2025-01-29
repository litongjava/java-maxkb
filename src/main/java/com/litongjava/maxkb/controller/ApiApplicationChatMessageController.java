package com.litongjava.maxkb.controller;

import com.litongjava.annotation.Post;
import com.litongjava.annotation.RequestPath;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.service.kb.MaxKbApplicationChatMessageService;
import com.litongjava.maxkb.vo.MaxKbChatRequestVo;
import com.litongjava.model.result.ResultVo;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.http.common.HeaderName;
import com.litongjava.tio.http.common.HeaderValue;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.server.util.CORSUtils;
import com.litongjava.tio.server.ServerChannelContext;
import com.litongjava.tio.utils.json.JsonUtils;

@RequestPath("/api/application/chat_message")
public class ApiApplicationChatMessageController {

  @Post("/{chatId}")
  public HttpResponse ask(Long chatId, HttpRequest request, ServerChannelContext channelContext) {
    String bodyString = request.getBodyString();
    MaxKbChatRequestVo maxKbChatRequestVo = JsonUtils.parse(bodyString, MaxKbChatRequestVo.class);
    HttpResponse httpResponse = TioRequestContext.getResponse();
    CORSUtils.enableCORS(httpResponse);
    // 设置sse请求头
    httpResponse.addServerSentEventsHeader();
    // 设置响应头
    httpResponse.addHeader(HeaderName.Transfer_Encoding, HeaderValue.from("chunked"));
    httpResponse.addHeader(HeaderName.Keep_Alive, HeaderValue.from("timeout=60"));
    // 手动发送消息到客户端,因为已经设置了sse的请求头,所以客户端的连接不会关闭
    Tio.bSend(channelContext, httpResponse);
    httpResponse.setSend(false);
    ResultVo resultVo = Aop.get(MaxKbApplicationChatMessageService.class).ask(channelContext, chatId, maxKbChatRequestVo);
    return httpResponse.setJson(resultVo);
  }
}
