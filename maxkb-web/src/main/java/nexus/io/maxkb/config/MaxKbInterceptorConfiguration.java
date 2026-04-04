package nexus.io.maxkb.config;

import com.litongjava.tio.boot.admin.consts.TioBootAdminUrls;

import nexus.io.maxkb.inteceptor.MaxKbAuthInterceptor;
import nexus.io.tio.boot.http.interceptor.HttpInteceptorConfigure;
import nexus.io.tio.boot.http.interceptor.HttpInterceptorModel;
import nexus.io.tio.boot.server.TioBootServer;

public class MaxKbInterceptorConfiguration {

  public void config() {
    MaxKbAuthInterceptor authTokenInterceptor = new MaxKbAuthInterceptor();
    HttpInterceptorModel model = new HttpInterceptorModel();
    model.setInterceptor(authTokenInterceptor);

    model.addBlockUrl("/**");

    model.addAllowUrls(TioBootAdminUrls.ALLLOW_URLS);
    model.addAllowUrls("", "/");
    model.addAllowUrls("/ui/**");
    model.addAllowUrl("/sse");

    model.addAllowUrls("/api/application/chat_message/*", "/api/profile", "/api/application/authentication");

    model.addAllowUrls("/api/v1/search", "/api/display/info", "/api/auth/types", "/api/qr_type");
    //
    HttpInteceptorConfigure configure = new HttpInteceptorConfigure();
    configure.add(model);

    TioBootServer.me().setHttpInteceptorConfigure(configure);
  }
}
