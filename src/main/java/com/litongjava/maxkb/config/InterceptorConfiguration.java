package com.litongjava.maxkb.config;

import com.litongjava.annotation.AConfiguration;
import com.litongjava.annotation.Initialization;
import com.litongjava.maxkb.inteceptor.AuthInterceptor;
import com.litongjava.tio.boot.http.interceptor.HttpInteceptorConfigure;
import com.litongjava.tio.boot.http.interceptor.HttpInterceptorModel;
import com.litongjava.tio.boot.server.TioBootServer;

@AConfiguration
public class InterceptorConfiguration {

  @Initialization
  public void config() {
    AuthInterceptor authTokenInterceptor = new AuthInterceptor();
    HttpInterceptorModel model = new HttpInterceptorModel();
    model.setInterceptor(authTokenInterceptor);

    model.addBlockUrl("/**");

    model.addAllowUrls("", "/");
    model.addAllowUrls("/ui/**");
    model.addAllowUrls("/register/*", "/api/login/account", "/api/login/outLogin", "/api/user/login");
    model.addAllowUrl("/sse");
    model.addAllowUrl("/api/application/chat_message/*");

    HttpInteceptorConfigure configure = new HttpInteceptorConfigure();
    configure.add(model);

    TioBootServer.me().setHttpInteceptorConfigure(configure);
  }
}
