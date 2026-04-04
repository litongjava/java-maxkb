package com.litongjava.maxkb.service.kb;

import com.litongjava.maxkb.constant.MaxKbKeysConst;
import com.litongjava.tio.utils.environment.EnvUtils;

import nexus.io.bailian.BaiLianAiModels;
import nexus.io.consts.ModelPlatformName;

public class MaxKbEnvUtils {

  public static String getEmbeddingPlatform() {
    return EnvUtils.getStr(MaxKbKeysConst.maxKbEmbddingPlatform, ModelPlatformName.BAILIAN);

  }

  public static String getEmbeddingModel() {
    return EnvUtils.getStr(MaxKbKeysConst.maxKbEmbddingModel, BaiLianAiModels.TEXT_EMBEDDING_V4);
  }

}
