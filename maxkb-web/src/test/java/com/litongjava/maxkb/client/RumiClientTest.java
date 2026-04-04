package com.litongjava.maxkb.client;

import org.junit.Test;

import nexus.io.jfinal.aop.Aop;

public class RumiClientTest {

  @Test
  public void test() {
    String embedding = Aop.get(RumiClient.class).embedding("HI");
    System.out.println(embedding);
  }

}
