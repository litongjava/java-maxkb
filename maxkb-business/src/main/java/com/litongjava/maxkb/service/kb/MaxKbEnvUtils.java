package com.litongjava.maxkb.service.kb;

import com.litongjava.bailian.BaiLianAiModels;
import com.litongjava.consts.ModelPlatformName;
import com.litongjava.maxkb.constant.MaxKbKeysConst;
import com.litongjava.tio.utils.environment.EnvUtils;

public class MaxKbEnvUtils {

  public static String getEmbeddingPlatform() {
    return EnvUtils.getStr(MaxKbKeysConst.maxKbEmbddingPlatform, ModelPlatformName.BAILIAN);

  }

  public static String getEmbeddingModel() {
    return EnvUtils.getStr(MaxKbKeysConst.maxKbEmbddingModel, BaiLianAiModels.TEXT_EMBEDDING_V4);
  }

}
