package com.litongjava.maxkb.model;

import org.junit.Test;
import org.postgresql.util.PGobject;

import nexus.io.db.activerecord.Db;
import nexus.io.kit.PgObjectUtils;
import nexus.io.maxkb.config.MaxKbDbConfig;
import nexus.io.maxkb.model.MaxKbApplicationTempSetting;
import nexus.io.maxkb.vo.MaxKbApplicationVo;
import nexus.io.tio.boot.testing.TioBootTest;
import nexus.io.tio.utils.json.JsonUtils;

public class MaxKbApplicationTempSettingTest {

  @Test
  public void test() {
    TioBootTest.runWith(MaxKbDbConfig.class);
    Long application_id = 446258395820601344L;
    PGobject pgObject = Db.queryPGobjectById(MaxKbApplicationTempSetting.tableName, "setting", application_id);
    System.out.println(pgObject.getValue());
    MaxKbApplicationVo bean = PgObjectUtils.toBean(pgObject, MaxKbApplicationVo.class);
    System.out.println(JsonUtils.toJson(bean));
  }

}
