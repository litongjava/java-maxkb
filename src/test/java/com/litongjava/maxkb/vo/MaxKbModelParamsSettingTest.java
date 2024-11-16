package com.litongjava.maxkb.vo;

import org.junit.Test;

import com.litongjava.tio.utils.json.JsonUtils;

public class MaxKbModelParamsSettingTest {

  @Test
  public void test() {
    MaxKbModelParamsSetting maxKbModelParamsSetting = new MaxKbModelParamsSetting();
    JsonUtils.toJson(maxKbModelParamsSetting);
  }

}
