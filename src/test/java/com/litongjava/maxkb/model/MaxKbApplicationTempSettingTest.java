package com.litongjava.maxkb.model;

import org.junit.Test;
import org.postgresql.util.PGobject;

import com.litongjava.db.activerecord.Db;
import com.litongjava.kit.JsonFieldUtils;
import com.litongjava.maxkb.config.DbConfig;
import com.litongjava.maxkb.vo.MaxKbApplicationVo;
import com.litongjava.tio.boot.testing.TioBootTest;
import com.litongjava.tio.utils.json.JsonUtils;

public class MaxKbApplicationTempSettingTest {

  @Test
  public void test() {
    TioBootTest.runWith(DbConfig.class);
    Long application_id = 446258395820601344L;
    PGobject pgObject = Db.queryPGobjectById(MaxKbApplicationTempSetting.tableName, "setting", application_id);
    System.out.println(pgObject.getValue());
    MaxKbApplicationVo bean = JsonFieldUtils.toBean(pgObject, MaxKbApplicationVo.class);
    System.out.println(JsonUtils.toJson(bean));
  }

}
