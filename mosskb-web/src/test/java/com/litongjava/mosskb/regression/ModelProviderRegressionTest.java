package com.litongjava.mosskb.regression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Test;

import nexus.io.db.activerecord.Db;
import nexus.io.jfinal.aop.Aop;
import nexus.io.mosskb.dao.ModelDao;
import nexus.io.mosskb.vo.CredentialVo;
import nexus.io.mosskb.vo.ModelVo;
import nexus.io.tio.utils.snowflake.SnowflakeIdUtils;

/**
 * 平台有效性回归测试。
 *
 * <p>模型必须挂在一个启用中的平台上。历史上有测试直接调 ModelDao 往生产库写
 * provider='provider' 的模型，这种平台在 moss_kb_model_provider 里并不存在，结果在选择
 * 列表里挂到空分组、还和别的模型重名。这里锁住两件事：DAO 不再允许写出悬空 provider，
 * 读取列表也不会把已经存在的悬空模型吐出来。
 */
public class ModelProviderRegressionTest {

  @BeforeClass
  public static void setUp() {
    RegressionDb.init();
  }

  /** 写一个平台不存在的模型必须被拒绝，而且不能真的落库。 */
  @Test
  public void modelWithUnknownProviderIsRejected() {
    ModelVo model = new ModelVo();
    model.setName("regression-unknown-provider");
    model.setModel_type("EMBEDDING");
    model.setModel_name("regression-unknown-provider");
    model.setPermission_type("PRIVATE");
    model.setProvider("provider_that_does_not_exist");
    model.setCredential(new CredentialVo("https://api.openai.com/v1", "sk-regression"));

    try {
      Aop.get(ModelDao.class).saveOrUpdate(0L, model);
      fail("平台不存在的模型不应该被写入");
    } catch (IllegalArgumentException expected) {
      assertTrue("异常信息应该点出供应商无效", expected.getMessage().contains("供应商"));
    }

    assertEquals(0, count("select count(*) from moss_kb_model where name = ?", "regression-unknown-provider"));
  }

  /**
   * 列表接口用的那套判据必须能把悬空模型筛掉。
   *
   * <p>MossKbModelService.list 需要 HTTP 请求上下文，进程内调不了，所以这里直接验证它依赖的
   * where 子句：造一条悬空模型，断言「有效模型」查询看不到它，而裸查询看得到。
   */
  @Test
  public void listPredicateExcludesModelsWhoseProviderIsGone() {
    Long id = SnowflakeIdUtils.id();
    String name = "regression-orphan-" + id;
    try {
      // 直接写库，绕过 DAO 的校验，模拟历史遗留的脏数据。
      Db.update("insert into moss_kb_model(id,name,model_type,model_name,provider,credential,user_id,permission_type,status)"
          + " values(?,?,?,?,?,?,0,'PRIVATE','SUCCESS')", id, name, "EMBEDDING", name, "provider_that_does_not_exist", "{}");

      assertTrue("前置条件失败：悬空模型没有写进去", count("select count(*) from moss_kb_model where id = ?", id) == 1);
      assertEquals("列表用的 where 子句应该把平台已失效的模型排除掉", 0,
          count("select count(*) from moss_kb_model m"
              + " where exists(select 1 from moss_kb_model_provider p where p.provider=m.provider and p.enabled=true)"
              + " and m.id = ?", id));
      assertEquals("按名称查询用的子句同样应该排除它", 0,
          count("select count(*) from moss_kb_model m"
              + " where exists(select 1 from moss_kb_model_provider p where p.provider=m.provider and p.enabled=true)"
              + " and m.name = ?", name));
    } finally {
      Db.deleteById("moss_kb_model", id);
    }
  }

  /** 库里不应该再留有平台已失效的模型。 */
  @Test
  public void noModelPointsAtAMissingProvider() {
    int orphan = count("select count(*) from moss_kb_model m"
        + " where not exists(select 1 from moss_kb_model_provider p where p.provider = m.provider and p.enabled = true)");
    assertEquals("存在平台已失效的模型，先清理 moss_kb_model 里的悬空数据", 0, orphan);
  }

  private static int count(String sql, Object... paras) {
    Integer value = Db.queryInt(sql, paras);
    return value == null ? -1 : value.intValue();
  }
}
