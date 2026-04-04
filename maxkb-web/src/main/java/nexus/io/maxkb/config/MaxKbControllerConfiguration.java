package nexus.io.maxkb.config;

import java.util.ArrayList;
import java.util.List;

import nexus.io.maxkb.controller.ApiApplicationChatController;
import nexus.io.maxkb.controller.ApiApplicationChatMessageController;
import nexus.io.maxkb.controller.ApiApplicationController;
import nexus.io.maxkb.controller.ApiAuthCASController;
import nexus.io.maxkb.controller.ApiAuthLDAPController;
import nexus.io.maxkb.controller.ApiAuthOAUTH2Controller;
import nexus.io.maxkb.controller.ApiAuthOIDCController;
import nexus.io.maxkb.controller.ApiAuthTypesController;
import nexus.io.maxkb.controller.ApiDatasetController;
import nexus.io.maxkb.controller.ApiDatasetDocumentController;
import nexus.io.maxkb.controller.ApiDisplayController;
import nexus.io.maxkb.controller.ApiEmailSettingController;
import nexus.io.maxkb.controller.ApiFunctionLibController;
import nexus.io.maxkb.controller.ApiModelController;
import nexus.io.maxkb.controller.ApiPlatformController;
import nexus.io.maxkb.controller.ApiProfileController;
import nexus.io.maxkb.controller.ApiProviderController;
import nexus.io.maxkb.controller.ApiQrTypeController;
import nexus.io.maxkb.controller.ApiTableMaxKbParagraphController;
import nexus.io.maxkb.controller.ApiTeamController;
import nexus.io.maxkb.controller.ApiUserController;
import nexus.io.maxkb.controller.ApiUserManage;
import nexus.io.maxkb.controller.ApiValidController;
import nexus.io.tio.boot.http.handler.controller.TioBootHttpControllerRouter;
import nexus.io.tio.boot.server.TioBootServer;

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
