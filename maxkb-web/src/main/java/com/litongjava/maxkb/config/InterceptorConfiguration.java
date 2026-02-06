package com.litongjava.maxkb.config;

import com.litongjava.maxkb.inteceptor.AuthInterceptor;
import com.litongjava.tio.boot.http.interceptor.HttpInteceptorConfigure;
import com.litongjava.tio.boot.http.interceptor.HttpInterceptorModel;
import com.litongjava.tio.boot.server.TioBootServer;

public class InterceptorConfiguration {

  public void config() {
    AuthInterceptor authTokenInterceptor = new AuthInterceptor();
    HttpInterceptorModel model = new HttpInterceptorModel();
    model.setInterceptor(authTokenInterceptor);

    model.addBlockUrl("/**");

    model.addAllowUrls("", "/");
    model.addAllowUrls("/ui/**");
    model.addAllowUrls("/register/*", "/api/login/account", "/api/login/outLogin", "/api/user/login");
    model.addAllowUrl("/sse");

    model.addAllowUrls("/api/application/chat_message/*", "/api/profile", "/api/application/authentication");

    model.addAllowUrls("/api/v1/search");

    HttpInteceptorConfigure configure = new HttpInteceptorConfigure();
    configure.add(model);

    TioBootServer.me().setHttpInteceptorConfigure(configure);
  }
}
