package com.litongjava.maxkb.controller;

import com.litongjava.annotation.RequestPath;
import com.litongjava.db.activerecord.Db;
import com.litongjava.model.body.RespBodyVo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestPath("/")
public class IndexController {
  @RequestPath()
  public String index() {
    log.info("close loader:{}", this.getClass().getClassLoader());
    return "index";
  }

  @RequestPath("db")
  public RespBodyVo db() {
    for (int i = 0; i < 10; i++) {
      Db.queryInt("select 1");
    }
    return RespBodyVo.ok();
  }
}
