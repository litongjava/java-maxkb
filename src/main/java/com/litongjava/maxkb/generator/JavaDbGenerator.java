package com.litongjava.maxkb.generator;

import javax.sql.DataSource;

import com.litongjava.db.activerecord.dialect.PostgreSqlDialect;
import com.litongjava.db.activerecord.generator.Generator;
import com.litongjava.db.druid.DruidPlugin;
import com.litongjava.tio.utils.environment.EnvUtils;

public class JavaDbGenerator {
  public static String modelPackageName = "com.litongjava.maxkb.model";

  public static void main(String[] args) {
    EnvUtils.load();
    DataSource dataSource = getDataSource();

    // BaseModel 的包名
    String baseModelPackageName = modelPackageName + ".base";
    // BaseModel 的输出路径
    String baseModelOutputDir = getBaseModelOutputDir(baseModelPackageName);

    // Model 的输出路径 (MappingKit 与 DataDictionary 默认保存路径)
    String modelOutputDir = baseModelOutputDir + "/..";

    // 创建生成器
    Generator generator = new Generator(dataSource, baseModelPackageName, baseModelOutputDir, modelPackageName, modelOutputDir);

    // 配置生成器
    generator.setGenerateRemarks(true); // 生成字段备注
    generator.setDialect(new PostgreSqlDialect()); // 设置数据库方言
    generator.setGenerateChainSetter(true); // 生成链式 setter 方法
    // generator.addExcludedTable("t_db_connect_info"); // 添加不需要生成的表名
    generator.setGenerateDaoInModel(true); // 在 Model 中生成 dao 对象
    generator.setGenerateDataDictionary(false); // 不生成数据字典
    generator.setRemovedTableNamePrefixes("t_"); // 移除表名前缀，如 "t_"，生成的 Model 名为 "User" 而非 "TUser"
    generator.addWhitelist("max_kb_user", "max_kb_user_token", "max_kb_model",
        //
        "max_kb_application", "max_kb_application_access_token",
        //
        "max_kb_application_chat", "max_kb_application_chat_record", "max_kb_application_temp_setting",
        //
        "max_kb_task", "max_kb_file", "max_kb_dataset", "max_kb_application_dataset_mapping", "max_kb_document",
        //
        "max_kb_paragraph", "max_kb_problem", "max_kb_problem_paragraph_mapping",
        //
        "max_kb_embedding_cache", "max_kb_document_markdown_cache", "max_kb_document_markdown_page_cache"

    );

    // 开始生成
    generator.generate();
  }

  public static String getBaseModelOutputDir(String modelPackageName) {
    String replace = modelPackageName.replace('.', '/');
    return "src/main/java/" + replace;
  }

  public static DruidPlugin createDruidPlugin() {
    String url = EnvUtils.get("jdbc.url").trim();
    String user = EnvUtils.get("jdbc.user").trim();
    String pswd = EnvUtils.get("jdbc.pswd").trim();
    return new DruidPlugin(url, user, pswd);
  }

  public static DataSource getDataSource() {
    DruidPlugin druidPlugin = createDruidPlugin();
    druidPlugin.start();
    return druidPlugin.getDataSource();
  }
}
