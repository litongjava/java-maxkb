package nexus.io.maxkb.config;

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
public class MaxKbDbConfig {

  public void config() {
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
    // add sql file
    // arp.addSqlTemplate("/sql/all_sqls.sql");
    // start
    arp.start();
    // add stop
    HookCan.me().addDestroyMethod(() -> {
      arp.stop();
      hikariCpPlugin.stop();
    });
  }
}
