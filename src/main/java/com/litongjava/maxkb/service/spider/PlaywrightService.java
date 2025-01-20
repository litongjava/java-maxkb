package com.litongjava.maxkb.service.spider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.litongjava.maxkb.playwright.PlaywrightBrowser;
import com.litongjava.searxng.WebPageConteont;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.utils.hutool.FilenameUtils;
import com.litongjava.tio.utils.thread.TioThreadUtils;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PlaywrightService {

  public StringBuffer spider(ChannelContext channelContext, long answerMessageId, List<WebPageConteont> pages) {
    //5.获取内容
    StringBuffer pageContents = new StringBuffer();
    for (int i = 0; i < pages.size(); i++) {
      String link = pages.get(i).getUrl();
      String suffix = FilenameUtils.getSuffix(link);
      if ("pdf".equals(suffix)) {
        log.info("skip:{}", suffix);
      } else {
        String bodyText = null;
        try {
          bodyText = PlaywrightBrowser.getBodyContent(link);
        } catch (Exception e) {
          log.error(e.getMessage(), e);
          continue;
        }
        pageContents.append("source " + (i + 1) + " " + bodyText).append("\n\n");
      }
    }
    return pageContents;
  }

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
        if (result != null) {
          pages.get(i).setContent(result);
        }
      } catch (InterruptedException | ExecutionException e) {
        log.error("Error retrieving task result: {}", e.getMessage(), e);
      }
    }
    return pages;
  }

  private String getPageContent(String link) {
    BrowserContext context = PlaywrightBrowser.acquire();
    try (Page page = context.newPage()) {
      page.navigate(link);
      String bodyText = page.innerText("body");
      return bodyText;
    } catch (Exception e) {
      log.error("Error getting content from {}: {}", link, e.getMessage(), e);
      return "";
    } finally {
      PlaywrightBrowser.release(context);
    }
  }
}
