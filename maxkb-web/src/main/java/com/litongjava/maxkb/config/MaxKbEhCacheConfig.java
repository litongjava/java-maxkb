package com.litongjava.maxkb.config;

import com.litongjava.ehcache.EhCachePlugin;

import nexus.io.hook.HookCan;

public class MaxKbEhCacheConfig {

  public void config() {
    EhCachePlugin ehCachePlugin = new EhCachePlugin();
    ehCachePlugin.start();
    HookCan.me().addDestroyMethod(ehCachePlugin::stop);
  }
}
