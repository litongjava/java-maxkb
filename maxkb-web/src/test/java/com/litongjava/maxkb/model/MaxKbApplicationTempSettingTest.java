package com.litongjava.maxkb.model;

import org.junit.Test;
import org.postgresql.util.PGobject;

import com.litongjava.maxkb.config.MaxKbDbConfig;
import com.litongjava.maxkb.vo.MaxKbApplicationVo;
import com.litongjava.tio.utils.json.JsonUtils;

import nexus.io.db.activerecord.Db;
import nexus.io.kit.PgObjectUtils;
import nexus.io.tio.boot.testing.TioBootTest;

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
