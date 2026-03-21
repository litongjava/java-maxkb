package com.litongjava.maxkb.service.kb;

import org.junit.Test;

import com.litongjava.chat.PlatformInput;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.config.boot.MaxKbBootConfig;
import com.litongjava.tio.boot.admin.utils.PrintlnUtils;
import com.litongjava.tio.boot.testing.TioBootTest;

public class MaxKbModelServiceTest {

  @Test
  public void getEmbeddingPlatformInput() {
    TioBootTest.runWith(new MaxKbBootConfig());
    
    PlatformInput embeddingPlatformInput = Aop.get(MaxKbModelService.class).getEmbeddingPlatformInput(01L);
    
    PrintlnUtils.printJson(embeddingPlatformInput);
  }

}
