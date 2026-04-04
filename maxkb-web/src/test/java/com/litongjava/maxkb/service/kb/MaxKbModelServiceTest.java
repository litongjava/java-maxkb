package com.litongjava.maxkb.service.kb;

import org.junit.Test;

import com.litongjava.tio.boot.admin.utils.PrintlnUtils;

import nexus.io.chat.PlatformInput;
import nexus.io.jfinal.aop.Aop;
import nexus.io.maxkb.config.boot.MaxKbBootConfig;
import nexus.io.maxkb.service.kb.MaxKbModelService;
import nexus.io.tio.boot.testing.TioBootTest;

public class MaxKbModelServiceTest {

  @Test
  public void getEmbeddingPlatformInput() {
    TioBootTest.runWith(new MaxKbBootConfig());
    
    PlatformInput embeddingPlatformInput = Aop.get(MaxKbModelService.class).getEmbeddingPlatformInput(01L);
    
    PrintlnUtils.printJson(embeddingPlatformInput);
  }

}
