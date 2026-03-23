package com.litongjava.maxkb.config.boot;

import com.litongjava.context.BootConfiguration;
import com.litongjava.maxkb.config.MaxKbDbConfig;
import com.litongjava.maxkb.config.MaxKbEhCacheConfig;
import com.litongjava.maxkb.config.MaxKbEnjoyEngineConfig;
import com.litongjava.maxkb.config.MaxKbFastJsonConfig;
import com.litongjava.maxkb.config.MaxKbHandlerConfiguration;
import com.litongjava.maxkb.config.MaxKbInterceptorConfiguration;
import com.litongjava.maxkb.config.MaxKbPlaywrightConfig;
import com.litongjava.maxkb.config.MaxKbQuartzConfig;
import com.litongjava.maxkb.config.MaxKbTioServerConfig;
import com.litongjava.maxkb.config.MaxKbTokenStoreConfig;

public class MaxKbBootConfig implements BootConfiguration {

  @Override
  public void config() throws Exception {
    System.setProperty("net.sf.ehcache.skipUpdateCheck", "true");
    
    new MaxKbDbConfig().config();
    new MaxKbEhCacheConfig().config();
    new MaxKbEnjoyEngineConfig().config();
    new MaxKbFastJsonConfig().config();
    new MaxKbHandlerConfiguration().config();
    new MaxKbInterceptorConfiguration().config();
    new MaxKbPlaywrightConfig().config();
    new MaxKbQuartzConfig().config();
    new MaxKbTioServerConfig().config();
    new MaxKbTokenStoreConfig().config();
  }
}
