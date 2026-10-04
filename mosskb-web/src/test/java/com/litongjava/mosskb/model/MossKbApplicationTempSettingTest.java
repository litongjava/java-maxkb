package com.litongjava.mosskb.model;

import org.junit.Test;
import org.postgresql.util.PGobject;

import nexus.io.db.activerecord.Db;
import nexus.io.kit.PgObjectUtils;
import nexus.io.mosskb.config.MossKbDbConfig;
import nexus.io.mosskb.model.MossKbApplicationTempSetting;
import nexus.io.mosskb.vo.MossKbApplicationVo;
import nexus.io.tio.boot.testing.TioBootTest;
import nexus.io.tio.utils.json.JsonUtils;

public class MossKbApplicationTempSettingTest {

  @Test
  public void test() {
    TioBootTest.runWith(MossKbDbConfig.class);
    Long application_id = 446258395820601344L;
    PGobject pgObject = Db.queryPGobjectById(MossKbApplicationTempSetting.tableName, "setting", application_id);
    System.out.println(pgObject.getValue());
    MossKbApplicationVo bean = PgObjectUtils.toBean(pgObject, MossKbApplicationVo.class);
    System.out.println(JsonUtils.toJson(bean));
  }

}
