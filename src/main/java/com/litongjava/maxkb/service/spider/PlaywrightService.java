package com.litongjava.maxkb.service.spider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;

import com.google.common.util.concurrent.Striped;
import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.maxkb.playwright.PlaywrightBrowser;
import com.litongjava.searxng.WebPageConteont;
import com.litongjava.tio.utils.hutool.FilenameUtils;
import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.utils.snowflake.SnowflakeIdUtils;
import com.litongjava.tio.utils.thread.TioThreadUtils;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PlaywrightService {

  public List<WebPageConteont> spiderAsync(List<WebPageConteont> pages) {
    List<Future<String>> futures = new ArrayList<>();

    for (int i = 0; i < pages.size(); i++) {
      String link = pages.get(i).getUrl();

      Future<String> future = TioThreadUtils.submit(() -> {
        String suffix = FilenameUtils.getSuffix(link);
        if ("pdf".equalsIgnoreCase(suffix)) {
          log.info("skip:{}", suffix);
          return null;
        } else {
          return getPageContent(link);
        }
      });
      futures.add(i, future);
    }
    for (int i = 0; i < pages.size(); i++) {
      Future<String> future = futures.get(i);
      try {
        String result = future.get();
        if (StrUtil.isNotBlank(result)) {
          pages.get(i).setContent(result);
        }
      } catch (InterruptedException | ExecutionException e) {
        log.error("Error retrieving task result: {}", e.getMessage(), e);
      }
    }
    return pages;
  }

  //使用Guava的Striped锁，设置64个锁段
  private static final Striped<Lock> stripedLocks = Striped.lock(64);

  private String getPageContent(String link) {
    // 首先检查数据库中是否已存在该页面内容
    if (Db.exists("max_kb_web_page_cache", "url", link)) {
      // 假设 content 字段存储了页面内容
      return Db.queryStr("SELECT content FROM max_kb_web_page_cache WHERE url = ?", link);
    }

    // 获取与链接对应的锁并锁定
    Lock lock = stripedLocks.get(link);
    lock.lock();
    try {
      // 再次检查，防止其他线程已生成内容
      if (Db.exists("max_kb_web_page_cache", "url", link)) {
        return Db.queryStr("SELECT content FROM max_kb_web_page_cache WHERE url = ?", link);
      }
      // 使用 PlaywrightBrowser 获取页面内容
      BrowserContext context = PlaywrightBrowser.acquire();
      String bodyText = "";
      try (Page page = context.newPage()) {
        page.navigate(link);
        bodyText = page.innerText("body");
      } catch (Exception e) {
        log.error("Error getting content from {}: {}", link, e.getMessage(), e);
      } finally {
        PlaywrightBrowser.release(context);
      }
      // 将获取到的页面内容保存到数据库
      if (!bodyText.isEmpty()) {
        // 构造数据库实体或使用直接 SQL 插入
        Row newRow = new Row();
        newRow.set("id", SnowflakeIdUtils.id()).set("url", link).set("content", bodyText);
        Db.save("max_kb_web_page_cache", newRow);
      }

      return bodyText;
    } finally {
      lock.unlock();
    }
  }
}
