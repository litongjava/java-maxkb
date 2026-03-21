package com.litongjava.maxkb.config;

import com.litongjava.maxkb.handler.ApiRagDatasetRetrievalHandler;
import com.litongjava.maxkb.httphandler.SearxngSearchHandler;
import com.litongjava.tio.boot.admin.config.TioAdminControllerConfiguration;
import com.litongjava.tio.boot.admin.config.TioAdminHandlerConfiguration;
import com.litongjava.tio.boot.server.TioBootServer;
import com.litongjava.tio.http.server.router.HttpRequestRouter;

public class MaxKbHandlerConfiguration {
  public void config() {
    HttpRequestRouter router = TioBootServer.me().getRequestRouter();
    if (router == null) {
      return;
    }
    SearxngSearchHandler searxngSearchHandler = new SearxngSearchHandler();
    router.add("/api/v1/search", searxngSearchHandler::search);
    ApiRagDatasetRetrievalHandler apiRagDatasetHandler = new ApiRagDatasetRetrievalHandler();
    router.add("/api/v1/rag/dataset/retrieval", apiRagDatasetHandler::index);
    
    new TioAdminHandlerConfiguration().config();
    new TioAdminControllerConfiguration().config();
  }
}
