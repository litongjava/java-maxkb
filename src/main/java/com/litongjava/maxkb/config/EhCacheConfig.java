package com.litongjava.maxkb.config;

import com.litongjava.annotation.AConfiguration;
import com.litongjava.annotation.Initialization;
import com.litongjava.ehcache.EhCachePlugin;
import com.litongjava.hook.HookCan;

@AConfiguration
public class EhCacheConfig {

  @Initialization
  public void ehCachePlugin() {
    EhCachePlugin ehCachePlugin = new EhCachePlugin();
    ehCachePlugin.start();
    HookCan.me().addDestroyMethod(ehCachePlugin::stop);
  }
}
