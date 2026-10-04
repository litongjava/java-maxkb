package nexus.io.mosskb.config;

import nexus.io.ehcache.EhCachePlugin;
import nexus.io.hook.HookCan;

public class MossKbEhCacheConfig {

  public void config() {
    EhCachePlugin ehCachePlugin = new EhCachePlugin();
    ehCachePlugin.start();
    HookCan.me().addDestroyMethod(ehCachePlugin::stop);
  }
}
