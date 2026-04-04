package nexus.io.maxkb.inteceptor;

import nexus.io.jfinal.aop.Aop;
import nexus.io.maxkb.service.MaxKbAuthService;
import nexus.io.tio.boot.http.TioRequestContext;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.http.common.HttpResponseStatus;
import nexus.io.tio.http.common.RequestLine;
import nexus.io.tio.http.server.intf.HttpRequestInterceptor;

public class MaxKbAuthInterceptor implements HttpRequestInterceptor {

  private Object body = null;

  public MaxKbAuthInterceptor() {
  }

  public MaxKbAuthInterceptor(Object body) {
    this.body = body;
  }

  @Override
  public HttpResponse doBeforeHandler(HttpRequest request, RequestLine requestLine, HttpResponse responseFromCache) {
    String authorization = request.getHeader("authorization");

    MaxKbAuthService authService = Aop.get(MaxKbAuthService.class);
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
