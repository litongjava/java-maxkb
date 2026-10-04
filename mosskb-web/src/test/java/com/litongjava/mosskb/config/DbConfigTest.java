package com.litongjava.mosskb.config;

import org.junit.Test;

import nexus.io.db.activerecord.Db;
import nexus.io.mosskb.config.MossKbDbConfig;
import nexus.io.tio.utils.environment.EnvUtils;

public class DbConfigTest {

  @Test
  public void test() {
    EnvUtils.load();
    new MossKbDbConfig().config();
    for (int i=0;i<2;i++) {
      Db.queryInt("select 1");
    }
  }

}
