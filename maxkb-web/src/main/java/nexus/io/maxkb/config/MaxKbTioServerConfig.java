package nexus.io.maxkb.config;

import nexus.io.maxkb.handler.GlobalExceptionHandler;
import nexus.io.tio.boot.server.TioBootServer;

public class MaxKbTioServerConfig {

  public void config() {
    //TioBootServer.me().setForwardHandler(new MyRequestForwardHandler());
    TioBootServer.me().setExceptionHandler(new GlobalExceptionHandler());
  }
}
