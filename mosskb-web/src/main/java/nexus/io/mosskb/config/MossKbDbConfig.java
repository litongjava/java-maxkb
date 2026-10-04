package nexus.io.mosskb.config;

import com.jfinal.template.Engine;
import com.jfinal.template.source.ClassPathSourceFactory;

import lombok.extern.slf4j.Slf4j;
import nexus.io.db.activerecord.ActiveRecordPlugin;
import nexus.io.db.activerecord.OrderedFieldContainerFactory;
import nexus.io.db.activerecord.dialect.PostgreSqlDialect;
import nexus.io.db.hikaricp.HikariCpPlugin;
import nexus.io.hook.HookCan;
import nexus.io.tio.utils.environment.EnvUtils;

@Slf4j
public class MossKbDbConfig {

  /**
   * 连接池与 ActiveRecord 配置在一个 JVM 里只能建一次：ActiveRecordPlugin.start() 会把配置
   * 注册成全局唯一的名字，重复注册会抛 IllegalArgumentException: Config already exists: main。
   * 生产启动只会调用一次；测试里多个测试类会各自调用 config()，靠这个标记保证幂等。
   */
  private static boolean configured;

  public void config() {
    if (configured) {
      log.debug("数据库配置已存在，跳过重复初始化");
      return;
    }
    String jdbcUrl = EnvUtils.getStr("jdbc.url");
    String jdbcUser = EnvUtils.getStr("jdbc.user");
    String jdbcPswd = EnvUtils.getStr("jdbc.pswd");
    log.info("jdbc.url:{}", jdbcUrl);
    // 初始化 HikariCP 数据库连接池
    HikariCpPlugin hikariCpPlugin = new HikariCpPlugin(jdbcUrl, jdbcUser, jdbcPswd);
    hikariCpPlugin.start();

    // create arp
    ActiveRecordPlugin arp = new ActiveRecordPlugin(hikariCpPlugin);

    if (EnvUtils.isDev()) {
      arp.setDevMode(true);

    }

    boolean showSql = EnvUtils.getBoolean("jdbc.showSql", false);
    log.info("show sql:{}", showSql);
    arp.setShowSql(showSql);
    arp.setDialect(new PostgreSqlDialect());
    arp.setContainerFactory(new OrderedFieldContainerFactory());

    // config engine
    Engine engine = arp.getEngine();
    engine.setSourceFactory(new ClassPathSourceFactory());
    engine.setCompressorOn(' ');
    engine.setCompressorOn('\n');
    // start
    arp.start();
    configured = true;
    // add stop
    HookCan.me().addDestroyMethod(() -> {
      arp.stop();
      hikariCpPlugin.stop();
    });
  }
}
