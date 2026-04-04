package nexus.io.maxkb.config;

import nexus.io.hook.HookCan;
import nexus.io.maxkb.playwright.PlaywrightBrowser;
import nexus.io.tio.utils.environment.EnvUtils;

public class MaxKbPlaywrightConfig {

  public void config() {

    if (EnvUtils.getBoolean("kb.playwright.enable", false)) {
      // 启动
      PlaywrightBrowser.init();

      // 服务关闭时，自动关闭浏览器和 Playwright 实例
      HookCan.me().addDestroyMethod(() -> {
        PlaywrightBrowser.close();
      });
    }
  }
}
