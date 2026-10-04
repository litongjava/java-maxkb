package com.litongjava.mosskb.client;

import org.junit.Test;

import nexus.io.jfinal.aop.Aop;
import nexus.io.mosskb.client.RumiClient;

public class RumiClientTest {

  @Test
  public void test() {
    String embedding = Aop.get(RumiClient.class).embedding("HI");
    System.out.println(embedding);
  }

}
