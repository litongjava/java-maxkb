package com.litongjava.maxkb.config;

import org.junit.Test;

import com.litongjava.db.activerecord.Db;
import com.litongjava.tio.utils.environment.EnvUtils;

public class DbConfigTest {

  @Test
  public void test() {
    EnvUtils.load();
    new DbConfig().config();
    for (int i=0;i<2;i++) {
      Db.queryInt("select 1");
    }
  }

}
