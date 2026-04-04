package com.litongjava.maxkb.service;

import org.junit.Test;

import com.litongjava.maxkb.config.MaxKbDbConfig;
import com.litongjava.maxkb.service.kb.KbParagraphService;
import com.litongjava.tio.utils.environment.EnvUtils;

import nexus.io.jfinal.aop.Aop;

public class KbParagraphServiceTest {

  @Test
  public void test() {
    EnvUtils.load();
    new MaxKbDbConfig().config();
    // List<KbParagraph> embedding = Aop.get(KbParagraphService.class).embedding();

    Aop.get(KbParagraphService.class).embedding("07705a6f-6ca9-11ef-a738-706655b928b8");
  }

  @Test
  public void reEmbedingTest() {
    EnvUtils.load();
    new MaxKbDbConfig().config();
    Aop.get(KbParagraphService.class).reEmbedding();
  }

}
