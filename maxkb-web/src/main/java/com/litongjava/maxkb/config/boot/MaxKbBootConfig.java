package com.litongjava.maxkb.config.boot;

import com.litongjava.context.BootConfiguration;
import com.litongjava.maxkb.config.DbConfig;
import com.litongjava.maxkb.config.EhCacheConfig;
import com.litongjava.maxkb.config.EnjoyEngineConfig;
import com.litongjava.maxkb.config.FastJsonConfig;
import com.litongjava.maxkb.config.HandlerConfiguration;
import com.litongjava.maxkb.config.InterceptorConfiguration;
import com.litongjava.maxkb.config.PlaywrightConfig;
import com.litongjava.maxkb.config.QuartzConfig;
import com.litongjava.maxkb.config.TioServerConfig;
import com.litongjava.maxkb.config.TokenStoreConfig;

public class MaxKbBootConfig implements BootConfiguration {

  @Override
  public void config() throws Exception {
    System.setProperty("net.sf.ehcache.skipUpdateCheck", "true");
    
    new DbConfig().config();
    new EhCacheConfig().config();
    new EnjoyEngineConfig().config();
    new FastJsonConfig().config();
    new HandlerConfiguration().config();
    new InterceptorConfiguration().config();
    new PlaywrightConfig().config();
    new QuartzConfig().config();
    new TioServerConfig().config();
    new TokenStoreConfig().config();
  }
}
