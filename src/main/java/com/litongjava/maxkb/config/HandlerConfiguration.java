package com.litongjava.maxkb.config;

import com.litongjava.annotation.AConfiguration;
import com.litongjava.annotation.Initialization;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.httphandler.SystemFileHandler;
import com.litongjava.tio.boot.server.TioBootServer;
import com.litongjava.tio.http.server.router.HttpRequestRouter;

@AConfiguration
public class HandlerConfiguration {
  @Initialization
  public void config() {
    HttpRequestRouter router = TioBootServer.me().getRequestRouter();
    if (router == null) {
      return;
    }
    // 获取文件处理器实例并注册路由
    SystemFileHandler fileHandler = Aop.get(SystemFileHandler.class);
    router.add("/api/system/file/upload", fileHandler::upload);
    router.add("/api/system/file/url", fileHandler::getUrl);
  }
}
