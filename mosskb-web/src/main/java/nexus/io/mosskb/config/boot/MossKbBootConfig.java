package nexus.io.mosskb.config.boot;

import nexus.io.context.BootConfiguration;
import nexus.io.mosskb.config.MossKbDbConfig;
import nexus.io.mosskb.config.MossKbEhCacheConfig;
import nexus.io.mosskb.config.MossKbEnjoyEngineConfig;
import nexus.io.mosskb.config.MossKbFastJsonConfig;
import nexus.io.mosskb.config.MossKbHandlerConfiguration;
import nexus.io.mosskb.config.MossKbInterceptorConfiguration;
import nexus.io.mosskb.config.MossKbPlaywrightConfig;
import nexus.io.mosskb.config.MossKbQuartzConfig;
import nexus.io.mosskb.config.MossKbTioServerConfig;
import nexus.io.mosskb.config.MossKbTokenStoreConfig;

public class MossKbBootConfig implements BootConfiguration {

  @Override
  public void config() throws Exception {
    System.setProperty("net.sf.ehcache.skipUpdateCheck", "true");
    
    new MossKbDbConfig().config();
    new MossKbEhCacheConfig().config();
    new MossKbEnjoyEngineConfig().config();
    new MossKbFastJsonConfig().config();
    new MossKbHandlerConfiguration().config();
    new MossKbInterceptorConfiguration().config();
    new MossKbPlaywrightConfig().config();
    new MossKbQuartzConfig().config();
    new MossKbTioServerConfig().config();
    new MossKbTokenStoreConfig().config();
  }
}
