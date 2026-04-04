package nexus.io.maxkb.config;

import com.litongjava.tio.boot.admin.config.TioAdminControllerConfiguration;
import com.litongjava.tio.boot.admin.config.TioAdminHandlerConfiguration;

import nexus.io.maxkb.handler.ApiRagDatasetRetrievalTitleHandler;
import nexus.io.maxkb.httphandler.SearxngSearchHandler;
import nexus.io.tio.boot.server.TioBootServer;
import nexus.io.tio.http.server.router.HttpRequestRouter;

public class MaxKbHandlerConfiguration {
  public void config() {
    HttpRequestRouter router = TioBootServer.me().getRequestRouter();
    if (router == null) {
      return;
    }
    SearxngSearchHandler searxngSearchHandler = new SearxngSearchHandler();
    router.add("/api/v1/search", searxngSearchHandler::search);
    ApiRagDatasetRetrievalTitleHandler apiRagDatasetHandler = new ApiRagDatasetRetrievalTitleHandler();
    router.add("/api/v1/rag/dataset/retrieval/title", apiRagDatasetHandler::index);
    
    new TioAdminHandlerConfiguration().config();
    new TioAdminControllerConfiguration().config();
  }
}
