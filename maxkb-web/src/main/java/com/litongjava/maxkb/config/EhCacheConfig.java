package com.litongjava.maxkb.config;

import com.litongjava.ehcache.EhCachePlugin;
import com.litongjava.hook.HookCan;

public class EhCacheConfig {

  public void config() {
    EhCachePlugin ehCachePlugin = new EhCachePlugin();
    ehCachePlugin.start();
    HookCan.me().addDestroyMethod(ehCachePlugin::stop);
  }
}
