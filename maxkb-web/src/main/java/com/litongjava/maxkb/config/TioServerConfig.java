package com.litongjava.maxkb.config;

import com.litongjava.maxkb.handler.GlobalExceptionHandler;
import com.litongjava.tio.boot.server.TioBootServer;

public class TioServerConfig {

  public void config() {
    //TioBootServer.me().setForwardHandler(new MyRequestForwardHandler());
    TioBootServer.me().setExceptionHandler(new GlobalExceptionHandler());
  }
}
