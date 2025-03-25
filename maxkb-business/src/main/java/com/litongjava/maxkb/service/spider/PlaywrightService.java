package com.litongjava.maxkb.service.spider;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;

import com.google.common.util.concurrent.Striped;
import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.maxkb.playwright.PlaywrightBrowser;
import com.litongjava.maxkb.utils.PdfUtils;
import com.litongjava.model.web.WebPageContent;
import com.litongjava.tio.utils.http.HttpDownloadUtils;
import com.litongjava.tio.utils.hutool.FilenameUtils;
import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.utils.snowflake.SnowflakeIdUtils;
import com.litongjava.tio.utils.thread.TioThreadUtils;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PlaywrightService {

  public static final String cache_table_name = "web_page_cache";

  // 使用Guava的Striped锁，设置64个锁段
  private static final Striped<Lock> stripedLocks = Striped.lock(1024);

  public List<WebPageContent> spiderAsync(List<WebPageContent> pages) {
    List<Future<String>> futures = new ArrayList<>();

    for (int i = 0; i < pages.size(); i++) {
      String link = pages.get(i).getUrl();

      Future<String> future = TioThreadUtils.submit(() -> {
        String suffix = FilenameUtils.getSuffix(link);
        if ("pdf".equalsIgnoreCase(suffix)) {
          ByteArrayOutputStream stream = HttpDownloadUtils.download(link, null);
          byte[] byteArray = stream.toByteArray();
          return PdfUtils.parseContent(byteArray);
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

  private String getPageContent(String link) {
    // 首先检查数据库中是否已存在该页面内容
    if (Db.exists(cache_table_name, "url", link)) {
      // 假设 content 字段存储了页面内容
      return Db.queryStr("SELECT text FROM " + cache_table_name + " WHERE url = ?", link);
    }

    // 获取与链接对应的锁并锁定
    Lock lock = stripedLocks.get(link);
    lock.lock();
    try {
      // 再次检查，防止其他线程已生成内容
      if (Db.exists(cache_table_name, "url", link)) {
        return Db.queryStr("SELECT text FROM " + cache_table_name + " WHERE url = ?", link);
      }
      // 使用 PlaywrightBrowser 获取页面内容
      BrowserContext context = PlaywrightBrowser.acquire();
      String html = null;
      String bodyText = null;
      try (Page page = context.newPage()) {
        page.navigate(link);
        bodyText = page.innerText("body");
        html = page.content();
      } catch (Exception e) {
        log.error("Error getting content from {}: {}", link, e.getMessage(), e);
      } finally {
        PlaywrightBrowser.release(context);
      }
      // 将获取到的页面内容保存到数据库
      if (bodyText != null && !bodyText.isEmpty()) {
        // 构造数据库实体或使用直接 SQL 插入
        Row newRow = new Row();
        newRow.set("id", SnowflakeIdUtils.id()).set("url", link).set("text", bodyText).set("html", html);
        Db.save(cache_table_name, newRow);
      }

      return bodyText;
    } finally {
      lock.unlock();
    }
  }

  public List<byte[]> screenshotScrollAsync(List<String> links) {
    List<Future<byte[]>> tasks = new ArrayList<>();
    for (int i = 0; i < links.size(); i++) {
      String link = links.get(i);
      Future<byte[]> submit = TioThreadUtils.submit(() -> {
        return screenshotScrollToBottom(link);
      });
      tasks.add(submit);
    }
    List<byte[]> retval = new ArrayList<>();
    for (int i = 0; i < tasks.size(); i++) {
      try {
        byte[] bs = tasks.get(i).get();
        if (bs != null) {
          retval.add(bs);
        }
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    }
    return retval;
  }

  /**
   * 异步滚动到底部并截图的方法
   *
   * @param link 要访问的网页链接
   * @return Future 包装的截图字节数组
   */
  public Future<byte[]> screenshotScrollToBottomAsync(String link) {
    return TioThreadUtils.submit(() -> screenshotScrollToBottom(link));
  }

  /**
   * 同步滚动到底部并截图的方法
   *
   * @param link 要访问的网页链接
   * @return 截图的字节数组（全页截图）
   */
  public byte[] screenshotScrollToBottom(String link) {
    log.info("start:{}", link);
    BrowserContext context = PlaywrightBrowser.acquire();
    try (Page page = context.newPage()) {
      page.navigate(link);
      // 调用辅助方法实现滚动到底部
      // scrollToBottom(page);
      // 截图全页面
      byte[] screenshot = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
      return screenshot;
    } catch (Exception e) {
      log.error("Error taking screenshot for {}: {}", link, e.getMessage(), e);
      return null;
    } finally {
      log.info("finish:{}", link);
      PlaywrightBrowser.release(context);
    }
  }

  /**
   * 利用页面执行 JavaScript 代码，循环滚动直到页面底部
   *
   * @param page Playwright 的 Page 对象
   */
  private void scrollToBottom(Page page) {
    // 通过 JS 代码循环滚动，直到页面滚动到底部
    page.evaluate("async () => {" + "  const distance = 100;" + "  const delay = 100;" + "  while (document.documentElement.scrollTop + window.innerHeight < document.documentElement.scrollHeight) {"
        + "    document.documentElement.scrollBy(0, distance);" + "    await new Promise(resolve => setTimeout(resolve, delay));" + "  }" + "}");
  }

  // ==============================
  // 以下为新增的 PDF 转换方法
  // ==============================

  /**
   * 同步将指定链接的网页转换为 PDF。
   * 注意：该功能仅在 Chromium 浏览器中有效。
   *
   * @param link 要转换的网页链接
   * @return PDF 文件的字节数组，如果转换失败则返回 null
   */
  public byte[] convertToPdf(String link) {
    log.info("开始将网页转换为 PDF: {}", link);
    BrowserContext context = PlaywrightBrowser.acquire();
    try (Page page = context.newPage()) {
      page.navigate(link);
      // 如果需要等待页面加载完毕，可以在此处添加等待逻辑

      // 生成 PDF，注意：PDF 生成功能仅在 Chromium 浏览器中支持
      byte[] pdf = page.pdf(new Page.PdfOptions().setFormat("A4") // 设置 PDF 的纸张格式，如 "A4" 或 "Letter"
          .setPrintBackground(true)); // 是否打印背景
      return pdf;
    } catch (Exception e) {
      log.error("将 {} 转换为 PDF 时发生错误: {}", link, e.getMessage(), e);
      return null;
    } finally {
      PlaywrightBrowser.release(context);
    }
  }

  public List<byte[]> convertToPdfAsync(List<String> links) {
    List<Future<byte[]>> tasks = new ArrayList<>();
    for (int i = 0; i < links.size(); i++) {
      String link = links.get(i);
      Future<byte[]> submit = TioThreadUtils.submit(() -> {
        return convertToPdf(link);
      });
      tasks.add(submit);
    }
    List<byte[]> retval = new ArrayList<>();
    for (int i = 0; i < tasks.size(); i++) {
      try {
        byte[] bs = tasks.get(i).get();
        if (bs != null) {
          retval.add(bs);
        }
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    }
    return retval;
  }

  /**
   * 异步将指定链接的网页转换为 PDF。
   *
   * @param link 要转换的网页链接
   * @return Future 包装的 PDF 字节数组
   */
  public Future<byte[]> convertToPdfAsync(String link) {
    return TioThreadUtils.submit(() -> convertToPdf(link));
  }
}