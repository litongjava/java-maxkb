package com.litongjava.mosskb.service;

import org.junit.Test;

import nexus.io.jfinal.aop.Aop;
import nexus.io.mosskb.config.MossKbDbConfig;
import nexus.io.mosskb.service.kb.KbParagraphService;
import nexus.io.tio.utils.environment.EnvUtils;

public class KbParagraphServiceTest {

  @Test
  public void test() {
    EnvUtils.load();
    new MossKbDbConfig().config();
    // List<KbParagraph> embedding = Aop.get(KbParagraphService.class).embedding();

    Aop.get(KbParagraphService.class).embedding("07705a6f-6ca9-11ef-a738-706655b928b8");
  }

  @Test
  public void reEmbedingTest() {
    EnvUtils.load();
    new MossKbDbConfig().config();
    Aop.get(KbParagraphService.class).reEmbedding();
  }

}
