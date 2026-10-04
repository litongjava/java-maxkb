package nexus.io.mosskb.service.kb;

import nexus.io.bailian.BaiLianAiModels;
import nexus.io.consts.ModelPlatformName;
import nexus.io.mosskb.constant.MossKbKeysConst;
import nexus.io.tio.utils.environment.EnvUtils;

public class MossKbEnvUtils {

  public static String getEmbeddingPlatform() {
    return EnvUtils.getStr(MossKbKeysConst.mossKbEmbddingPlatform, ModelPlatformName.BAILIAN);

  }

  public static String getEmbeddingModel() {
    return EnvUtils.getStr(MossKbKeysConst.mossKbEmbddingModel, BaiLianAiModels.TEXT_EMBEDDING_V4);
  }

}
