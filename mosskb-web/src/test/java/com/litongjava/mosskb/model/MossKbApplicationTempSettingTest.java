package com.litongjava.mosskb.model;

import org.junit.Assume;
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
    // 这个 application_id 来自作者本地的历史数据，新库上还没有对应记录时跳过。
    Assume.assumeTrue("临时设置表里没有应用 " + application_id + " 的记录，跳过", pgObject != null);

    System.out.println(pgObject.getValue());
    MossKbApplicationVo bean = PgObjectUtils.toBean(pgObject, MossKbApplicationVo.class);
    System.out.println(JsonUtils.toJson(bean));
  }

}
