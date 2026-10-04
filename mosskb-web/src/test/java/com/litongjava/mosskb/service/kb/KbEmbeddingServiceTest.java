package com.litongjava.mosskb.service.kb;

import org.junit.Test;

import nexus.io.chat.PlatformInput;
import nexus.io.jfinal.aop.Aop;
import nexus.io.mosskb.config.boot.MossKbBootConfig;
import nexus.io.mosskb.service.kb.KbEmbeddingService;
import nexus.io.mosskb.service.kb.MossKbModelService;
import nexus.io.tio.boot.testing.TioBootTest;

public class KbEmbeddingServiceTest {

  @Test
  public void test() {
    TioBootTest.runWith(new MossKbBootConfig());
    PlatformInput embeddingPlatformInput = Aop.get(MossKbModelService.class).getEmbeddingPlatformInput(01L);

    Long vectorId = Aop.get(KbEmbeddingService.class).getVectorId("123456", embeddingPlatformInput);
    System.out.println(vectorId);

  }

}
