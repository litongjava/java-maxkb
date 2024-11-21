package com.litongjava.maxkb.config;

import com.jfinal.template.Engine;
import com.jfinal.template.source.ClassPathSourceFactory;
import com.litongjava.annotation.AConfiguration;
import com.litongjava.annotation.Initialization;
import com.litongjava.db.activerecord.ActiveRecordPlugin;
import com.litongjava.db.activerecord.OrderedFieldContainerFactory;
import com.litongjava.db.activerecord.dialect.PostgreSqlDialect;
import com.litongjava.db.hikaricp.HikariCpPlugin;
import com.litongjava.tio.boot.server.TioBootServer;
import com.litongjava.tio.utils.environment.EnvUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@AConfiguration
public class DbConfig {
  @Initialization
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
    TioBootServer.me().addDestroyMethod(() -> {
      arp.stop();
      hikariCpPlugin.stop();
    });
  }
}
