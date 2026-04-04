package com.litongjava.maxkb.config;

import org.junit.Test;

import com.litongjava.tio.utils.environment.EnvUtils;

import nexus.io.db.activerecord.Db;

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
