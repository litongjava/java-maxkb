package nexus.io.maxkb.service.kb;

import nexus.io.bailian.BaiLianAiModels;
import nexus.io.consts.ModelPlatformName;
import nexus.io.maxkb.constant.MaxKbKeysConst;
import nexus.io.tio.utils.environment.EnvUtils;

public class MaxKbEnvUtils {

  public static String getEmbeddingPlatform() {
    return EnvUtils.getStr(MaxKbKeysConst.maxKbEmbddingPlatform, ModelPlatformName.BAILIAN);

  }

  public static String getEmbeddingModel() {
    return EnvUtils.getStr(MaxKbKeysConst.maxKbEmbddingModel, BaiLianAiModels.TEXT_EMBEDDING_V4);
  }

}
