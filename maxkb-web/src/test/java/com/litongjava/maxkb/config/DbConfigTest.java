package com.litongjava.maxkb.config;

import org.junit.Test;

import nexus.io.db.activerecord.Db;
import nexus.io.tio.utils.environment.EnvUtils;

public class DbConfigTest {

  @Test
  public void test() {
    EnvUtils.load();
    new MaxKbDbConfig().config();
    for (int i=0;i<2;i++) {
      Db.queryInt("select 1");
    }
  }

}
