package com.litongjava.maxkb.config;

import com.litongjava.annotation.AConfiguration;
import com.litongjava.annotation.Initialization;
import com.litongjava.maxkb.handler.GlobalExceptionHandler;
import com.litongjava.tio.boot.server.TioBootServer;

@AConfiguration
public class TioServerConfig {

  @Initialization
  public void config() {
    //TioBootServer.me().setForwardHandler(new MyRequestForwardHandler());
    TioBootServer.me().setExceptionHandler(new GlobalExceptionHandler());
  }
}
