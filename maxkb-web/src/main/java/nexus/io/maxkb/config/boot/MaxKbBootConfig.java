package nexus.io.maxkb.config.boot;

import nexus.io.context.BootConfiguration;
import nexus.io.maxkb.config.MaxKbDbConfig;
import nexus.io.maxkb.config.MaxKbEhCacheConfig;
import nexus.io.maxkb.config.MaxKbEnjoyEngineConfig;
import nexus.io.maxkb.config.MaxKbFastJsonConfig;
import nexus.io.maxkb.config.MaxKbHandlerConfiguration;
import nexus.io.maxkb.config.MaxKbInterceptorConfiguration;
import nexus.io.maxkb.config.MaxKbPlaywrightConfig;
import nexus.io.maxkb.config.MaxKbQuartzConfig;
import nexus.io.maxkb.config.MaxKbTioServerConfig;
import nexus.io.maxkb.config.MaxKbTokenStoreConfig;

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
