package com.litongjava.maxkb.config;

import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.quartz.QuartzUtils;

import nexus.io.hook.HookCan;

public class MaxKbQuartzConfig {

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
