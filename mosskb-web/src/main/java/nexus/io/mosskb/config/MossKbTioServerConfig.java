package nexus.io.mosskb.config;

import nexus.io.mosskb.handler.GlobalExceptionHandler;
import nexus.io.tio.boot.server.TioBootServer;

public class MossKbTioServerConfig {

  public void config() {
    //TioBootServer.me().setForwardHandler(new MyRequestForwardHandler());
    TioBootServer.me().setExceptionHandler(new GlobalExceptionHandler());
  }
}
