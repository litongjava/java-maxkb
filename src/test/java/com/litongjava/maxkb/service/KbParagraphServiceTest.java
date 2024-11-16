package com.litongjava.maxkb.service;

import org.junit.Test;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.config.DbConfig;
import com.litongjava.tio.utils.environment.EnvUtils;

public class KbParagraphServiceTest {

  @Test
  public void test() {
    EnvUtils.load();
    new DbConfig().config();
    // List<KbParagraph> embedding = Aop.get(KbParagraphService.class).embedding();

    Aop.get(KbParagraphService.class).embedding("07705a6f-6ca9-11ef-a738-706655b928b8");
  }

  @Test
  public void reEmbedingTest() {
    EnvUtils.load();
    new DbConfig().config();
    Aop.get(KbParagraphService.class).reEmbedding();
  }

}
