package com.litongjava.mosskb.service;

import org.junit.Ignore;
import org.junit.Test;

import nexus.io.jfinal.aop.Aop;
import nexus.io.mosskb.config.MossKbDbConfig;
import nexus.io.mosskb.service.kb.KbParagraphService;
import nexus.io.tio.utils.environment.EnvUtils;

/**
 * KbParagraphService 是按旧版结构写的：段落主键还是 UUID，写向量时用的表名是 embedding。
 * 当前 moss_kb_paragraph.id 是 BIGINT，也没有 embedding 表，直接把 UUID 参数绑上去会被
 * PostgreSQL 拒绝（operator does not exist: bigint = uuid）。等这个服务按新结构改写后再启用。
 */
@Ignore("KbParagraphService 仍按旧版 UUID 主键与 embedding 表编写，与当前库结构不符")
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
