package com.litongjava.maxkb.playwright;

import java.util.concurrent.TimeUnit;

import com.litongjava.tio.utils.environment.EnvUtils;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;

public enum PlaywrightBrowser {
  INSTANCE;

  // 定义池化管理器
  public static BrowserContextPool contextPool;
  static {
    // 初始化上下文池，假设池大小为10，可根据需要调整
    if(EnvUtils.isDev()) {
      contextPool = new BrowserContextPool(2);
    }else {
      contextPool = new BrowserContextPool(Runtime.getRuntime().availableProcessors() * 2);
    }
  }

  public static void init() {
  }

  public static void close() {
    // 关闭上下文池中的所有上下文
    contextPool.close();
  }

  public static String getHtml(String url) {
    BrowserContext context = null;
    Page page = null;
    String content = "";
    try {
      // 从池中获取一个上下文，最多等待5秒
      context = contextPool.acquire(5, TimeUnit.SECONDS);
      if (context == null) {
        throw new RuntimeException("无法获取 BrowserContext");
      }
      page = context.newPage();
      page.navigate(url);
      content = page.content();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("获取 BrowserContext 被中断", e);
    } finally {
      if (page != null) {
        page.close();
      }
      // 将上下文归还池中
      if (context != null) {
        contextPool.release(context);
      }
    }
    return content;
  }

  public static String getBodyContent(String url) {
    BrowserContext context = null;
    Page page = null;
    String textContent = "";
    try {
      context = contextPool.acquire(5, TimeUnit.SECONDS);
      if (context == null) {
        throw new RuntimeException("无法获取 BrowserContext");
      }
      page = context.newPage();
      page.navigate(url);
      textContent = page.innerText("body");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("获取 BrowserContext 被中断", e);
    } finally {
      if (page != null) {
        page.close();
      }
      if (context != null) {
        contextPool.release(context);
      }
    }
    return textContent;
  }

  public static BrowserContext acquire() {
    try {
      return contextPool.acquire(60, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public static void release(BrowserContext context) {
    contextPool.release(context);
  }
}
