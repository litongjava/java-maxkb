package com.litongjava.maxkb.config;

import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.reader.ObjectReaderProvider;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FastJsonConfig {

  public void config() {
    log.info("clean fastjson 2 cache");
    ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
    provider.clear();
  }
}
