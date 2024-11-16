package com.litongjava.maxkb.httphandler;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.service.AppForwardRequestService;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.boot.http.forward.TioHttpProxy;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.server.handler.HttpRequestHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MyRequestForwardHandler implements HttpRequestHandler {

  private String remoteServerUrl = "http://192.168.3.9:8080";

  @Override
  public HttpResponse handle(HttpRequest httpRequest) throws Exception {
    HttpResponse httpResponse = TioRequestContext.getResponse();
    AppForwardRequestService forwardRequestService = Aop.get(AppForwardRequestService.class);
    log.info("forward:{},{}", httpRequest.getRequestLine().toString(), httpRequest.logstr());
    TioHttpProxy.reverseProxy(remoteServerUrl, httpRequest, httpResponse, forwardRequestService);
    return httpResponse;
  }
}
