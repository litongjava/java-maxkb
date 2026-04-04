package com.litongjava.maxkb.vo;

import org.junit.Test;

import nexus.io.maxkb.vo.MaxKbModelParamsSetting;
import nexus.io.tio.utils.json.JsonUtils;

public class MaxKbModelParamsSettingTest {

  @Test
  public void test() {
    MaxKbModelParamsSetting maxKbModelParamsSetting = new MaxKbModelParamsSetting();
    JsonUtils.toJson(maxKbModelParamsSetting);
  }

}
