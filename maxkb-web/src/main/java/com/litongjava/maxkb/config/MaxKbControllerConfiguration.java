package com.litongjava.maxkb.config;

import java.util.ArrayList;
import java.util.List;

import com.litongjava.maxkb.controller.ApiApplicationChatController;
import com.litongjava.maxkb.controller.ApiApplicationChatMessageController;
import com.litongjava.maxkb.controller.ApiApplicationController;
import com.litongjava.maxkb.controller.ApiAuthCASController;
import com.litongjava.maxkb.controller.ApiAuthLDAPController;
import com.litongjava.maxkb.controller.ApiAuthOAUTH2Controller;
import com.litongjava.maxkb.controller.ApiAuthOIDCController;
import com.litongjava.maxkb.controller.ApiAuthTypesController;
import com.litongjava.maxkb.controller.ApiDatasetController;
import com.litongjava.maxkb.controller.ApiDatasetDocumentController;
import com.litongjava.maxkb.controller.ApiDisplayController;
import com.litongjava.maxkb.controller.ApiEmailSettingController;
import com.litongjava.maxkb.controller.ApiFunctionLibController;
import com.litongjava.maxkb.controller.ApiModelController;
import com.litongjava.maxkb.controller.ApiPlatformController;
import com.litongjava.maxkb.controller.ApiProfileController;
import com.litongjava.maxkb.controller.ApiProviderController;
import com.litongjava.maxkb.controller.ApiQrTypeController;
import com.litongjava.maxkb.controller.ApiTableMaxKbParagraphController;
import com.litongjava.maxkb.controller.ApiTeamController;
import com.litongjava.maxkb.controller.ApiUserController;
import com.litongjava.maxkb.controller.ApiUserManage;
import com.litongjava.maxkb.controller.ApiValidController;
import com.litongjava.tio.boot.http.handler.controller.TioBootHttpControllerRouter;
import com.litongjava.tio.boot.server.TioBootServer;

public class MaxKbControllerConfiguration {

  public void config() {
    TioBootHttpControllerRouter controllerRouter = TioBootServer.me().getControllerRouter();
    if (controllerRouter == null) {
      return;
    }
    List<Class<?>> scannedClasses = new ArrayList<>();
    scannedClasses.add(ApiApplicationChatController.class);
    scannedClasses.add(ApiApplicationChatMessageController.class);
    scannedClasses.add(ApiApplicationController.class);
    scannedClasses.add(ApiAuthCASController.class);
    scannedClasses.add(ApiAuthLDAPController.class);
    scannedClasses.add(ApiAuthOAUTH2Controller.class);
    scannedClasses.add(ApiAuthOIDCController.class);
    scannedClasses.add(ApiAuthTypesController.class);
    scannedClasses.add(ApiDatasetController.class);
    scannedClasses.add(ApiDatasetDocumentController.class);
    scannedClasses.add(ApiDisplayController.class);
    scannedClasses.add(ApiEmailSettingController.class);
    scannedClasses.add(ApiFunctionLibController.class);
    scannedClasses.add(ApiModelController.class);
    scannedClasses.add(ApiPlatformController.class);
    scannedClasses.add(ApiProfileController.class);
    scannedClasses.add(ApiProviderController.class);
    scannedClasses.add(ApiQrTypeController.class);
    scannedClasses.add(ApiTableMaxKbParagraphController.class);
    scannedClasses.add(ApiTeamController.class);
    scannedClasses.add(ApiUserController.class);
    scannedClasses.add(ApiUserManage.class);
    scannedClasses.add(ApiValidController.class);
    //scannedClasses.add(MongodbController.class);
    controllerRouter.addControllers(scannedClasses);
  }
}
