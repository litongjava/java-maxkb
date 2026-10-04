package com.litongjava.mosskb.client;

import org.junit.Ignore;
import org.junit.Test;

import nexus.io.jfinal.aop.Aop;
import nexus.io.mosskb.client.RumiClient;

public class RumiClientTest {

  /**
   * RumiClient 里的服务地址与密钥是写死的占位值（密钥为空），OpenAiClient 会直接抛
   * “api key can not empty”，因此这个用例在当前实现下不可能通过。要恢复它，先把 RumiClient
   * 的地址与密钥改成可配置。
   */
  @Ignore("RumiClient 内置占位地址与空密钥，改为可配置后再启用该联调用例")
  @Test
  public void test() {
    String embedding = Aop.get(RumiClient.class).embedding("HI");
    System.out.println(embedding);
  }

}
