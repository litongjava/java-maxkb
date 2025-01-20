package com.litongjava.maxkb.config;

import com.litongjava.annotation.AConfiguration;
import com.litongjava.annotation.Initialization;
import com.litongjava.hook.HookCan;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.quartz.QuartzUtils;

@AConfiguration
public class QuartzConfig {

  @Initialization
  public void config() {
    if (EnvUtils.isProd()) {
      // 启动 Quartz 定时任务
      QuartzUtils.start();

      // 在应用销毁时停止 Quartz
      HookCan.me().addDestroyMethod(() -> {
        QuartzUtils.stop();
      });
    }

  }
}
