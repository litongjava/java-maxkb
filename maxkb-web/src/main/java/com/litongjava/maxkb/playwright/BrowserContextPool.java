package com.litongjava.maxkb.playwright;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.BrowserType.LaunchOptions;
import com.microsoft.playwright.Playwright;

public class BrowserContextPool {
  private final BlockingQueue<Playwright> playwrightPool;
  private final BlockingQueue<Browser> brwoserPool;
  private final BlockingQueue<BrowserContext> browserContextPool;

  public BrowserContextPool(int poolSize) {
    this.playwrightPool = new LinkedBlockingQueue<>(poolSize);
    this.brwoserPool = new LinkedBlockingQueue<>(poolSize);
    this.browserContextPool = new LinkedBlockingQueue<>(poolSize);
    LaunchOptions launchOptions = new BrowserType.LaunchOptions().setHeadless(true);
    // 预先创建上下文并放入池中
    for (int i = 0; i < poolSize; i++) {
      Playwright playwright = Playwright.create();
      playwrightPool.offer(playwright);

      Browser brwoser = playwright.chromium().launch(launchOptions);
      brwoserPool.offer(brwoser);

      BrowserContext browserContext = brwoser.newContext();
      //browserContext.newPage();
      browserContextPool.offer(browserContext);
    }
  }

  /**
   * 从池中获取一个 BrowserContext。如果池为空，则等待指定时间后返回null。
   */
  public BrowserContext acquire(long timeout, TimeUnit unit) throws InterruptedException {
    return browserContextPool.poll(timeout, unit);
  }

  /**
   * 将使用完毕的 BrowserContext 归还到池中
   */
  public void release(BrowserContext context) {
    if (context != null) {
      browserContextPool.offer(context);
    }
  }

  /**
   * 释放池中所有的 BrowserContext 资源
   */
  public void close() {
    for (Playwright context : playwrightPool) {
      context.close();
    }
    playwrightPool.clear();
    for (Browser context : brwoserPool) {
      context.close();
    }
    brwoserPool.clear();

    for (BrowserContext context : browserContextPool) {
      context.close();
    }
    browserContextPool.clear();
  }
}

