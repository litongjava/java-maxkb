package com.litongjava.maxkb.config;

import com.litongjava.annotation.AConfiguration;
import com.litongjava.annotation.Initialization;
import com.litongjava.hook.HookCan;
import com.litongjava.maxkb.playwright.PlaywrightBrowser;

@AConfiguration
public class PlaywrightConfig {

  @Initialization
  public void config() {
    // 启动
    PlaywrightBrowser.init();

    // 服务关闭时，自动关闭浏览器和 Playwright 实例
    HookCan.me().addDestroyMethod(() -> {
      PlaywrightBrowser.close();
    });
  }
}
