package com.litongjava.mosskb.vo;

import org.junit.Test;

import nexus.io.mosskb.vo.MossKbModelParamsSetting;
import nexus.io.tio.utils.json.JsonUtils;

public class MossKbModelParamsSettingTest {

  @Test
  public void test() {
    MossKbModelParamsSetting mossKbModelParamsSetting = new MossKbModelParamsSetting();
    JsonUtils.toJson(mossKbModelParamsSetting);
  }

}
