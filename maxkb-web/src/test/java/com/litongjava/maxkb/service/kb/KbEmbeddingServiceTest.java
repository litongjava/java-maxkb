package com.litongjava.maxkb.service.kb;

import org.junit.Test;

import com.litongjava.chat.PlatformInput;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.config.boot.MaxKbBootConfig;
import com.litongjava.tio.boot.testing.TioBootTest;

public class KbEmbeddingServiceTest {

  @Test
  public void test() {
    TioBootTest.runWith(new MaxKbBootConfig());
    PlatformInput embeddingPlatformInput = Aop.get(MaxKbModelService.class).getEmbeddingPlatformInput(01L);

    Long vectorId = Aop.get(KbEmbeddingService.class).getVectorId("123456", embeddingPlatformInput);
    System.out.println(vectorId);

  }

}
