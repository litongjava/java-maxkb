package com.litongjava.maxkb.inteceptor;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.service.AuthService;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.HttpResponseStatus;
import com.litongjava.tio.http.common.RequestLine;
import com.litongjava.tio.http.server.intf.HttpRequestInterceptor;

public class AuthInterceptor implements HttpRequestInterceptor {

  private Object body = null;

  public AuthInterceptor() {
  }

  public AuthInterceptor(Object body) {
    this.body = body;
  }

  @Override
  public HttpResponse doBeforeHandler(HttpRequest request, RequestLine requestLine, HttpResponse responseFromCache) {
    String authorization = request.getHeader("authorization");

    AuthService authService = Aop.get(AuthService.class);
    Long userId = authService.getIdByToken(authorization);

    if (userId != null) {
      TioRequestContext.setUserId(userId);
      return null;
    }

    HttpResponse response = TioRequestContext.getResponse();
    response.setStatus(HttpResponseStatus.C401);
    
    if (body != null) {
      response.setJson(body);
    }
    return response;
  }

  @Override
  public void doAfterHandler(HttpRequest request, RequestLine requestLine, HttpResponse response, long cost) throws Exception {
  }
}
