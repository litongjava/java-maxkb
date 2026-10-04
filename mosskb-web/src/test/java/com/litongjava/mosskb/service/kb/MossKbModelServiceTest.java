package com.litongjava.mosskb.service.kb;

import org.junit.Test;

import nexus.io.chat.PlatformInput;
import nexus.io.jfinal.aop.Aop;
import nexus.io.mosskb.config.boot.MossKbBootConfig;
import nexus.io.mosskb.service.kb.MossKbModelService;
import nexus.io.tio.boot.admin.utils.PrintlnUtils;
import nexus.io.tio.boot.testing.TioBootTest;

public class MossKbModelServiceTest {

  @Test
  public void getEmbeddingPlatformInput() {
    TioBootTest.runWith(new MossKbBootConfig());
    
    PlatformInput embeddingPlatformInput = Aop.get(MossKbModelService.class).getEmbeddingPlatformInput(01L);
    
    PrintlnUtils.printJson(embeddingPlatformInput);
  }

}
