package com.litongjava.mosskb.regression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.alibaba.fastjson2.JSONObject;
import com.jfinal.kit.Kv;

import nexus.io.db.activerecord.Db;
import nexus.io.model.result.ResultVo;
import nexus.io.mosskb.service.KbUserService;
import nexus.io.mosskb.service.UserPassword;
import nexus.io.tio.utils.snowflake.SnowflakeIdUtils;

/**
 * 切换界面语言的回归测试。
 *
 * <p>前端头像菜单的语言下拉调用 POST /api/user/language。后端一旦没有这个接口，
 * 请求得到 404，而前端把接口 404 当成「页面不存在」，会直接把用户甩到 404 页面。
 * 这里固定住三件事：语言写进 moss_kb_user.language、GET /api/user 把 language 回给前端、
 * 非法语言被拒绝且不改动已存的值。
 *
 * <p>测试用独立账号，不动种子里的 admin。
 */
public class UserLanguageRegressionTest {

  private static final String USERNAME = "language_regression_user";

  private static final String SELECT_LANGUAGE = """
      select language
      from moss_kb_user
      where id = ?
      """;

  private static final String DELETE_USER = """
      delete from moss_kb_user
      where id = ?
      """;

  private Long userId;

  @BeforeClass
  public static void setUp() {
    RegressionDb.init();
  }

  @After
  public void cleanUp() {
    if (userId != null) {
      Db.update(DELETE_USER, userId);
      userId = null;
    }
  }

  @Test
  public void switchedLanguageIsPersistedAndReturnedInProfile() {
    long id = createUser();

    ResultVo result = new KbUserService().switchLanguage(id, JSONObject.of("language", "zh-CN"));

    assertEquals(200, result.getCode());
    assertEquals("切换后的语言没有落库", "zh-CN", Db.queryStr(SELECT_LANGUAGE, id));

    // 前端 profile() 用 GET /api/user 返回的 language 决定界面语言，缺了它切换结果带不回来。
    ResultVo profile = new KbUserService().index(id);
    assertEquals(200, profile.getCode());
    assertEquals("个人资料里没有带回 language", "zh-CN", ((Kv) profile.getData()).getStr("language"));
  }

  @Test
  public void traditionalChineseIsAcceptedAndCanBeSwitchedBack() {
    long id = createUser();

    assertEquals(200, new KbUserService().switchLanguage(id, JSONObject.of("language", "zh-Hant")).getCode());
    assertEquals("zh-Hant", Db.queryStr(SELECT_LANGUAGE, id));

    assertEquals(200, new KbUserService().switchLanguage(id, JSONObject.of("language", "en-US")).getCode());
    assertEquals("en-US", Db.queryStr(SELECT_LANGUAGE, id));
  }

  @Test
  public void unsupportedOrMissingLanguageIsRejected() {
    long id = createUser();

    ResultVo unsupported = new KbUserService().switchLanguage(id, JSONObject.of("language", "ja-JP"));
    assertEquals(400, unsupported.getCode());

    ResultVo missing = new KbUserService().switchLanguage(id, JSONObject.of());
    assertEquals(400, missing.getCode());

    ResultVo emptyBody = new KbUserService().switchLanguage(id, null);
    assertEquals(400, emptyBody.getCode());

    assertNull("被拒绝的请求不应该写库", Db.queryStr(SELECT_LANGUAGE, id));
  }

  @Test
  public void anonymousRequestIsRejected() {
    ResultVo result = new KbUserService().switchLanguage(null, JSONObject.of("language", "zh-CN"));

    assertEquals(400, result.getCode());
  }

  private long createUser() {
    long id = SnowflakeIdUtils.id();
    Db.update("""
        insert into moss_kb_user
          (id, email, phone, nick_name, username, password, role, is_active, source, deleted, token_version)
        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, id, "", "", "", USERNAME, UserPassword.hash("language-password-123"), "USER", true, "LOCAL", 0, 0);
    userId = id;
    return id;
  }
}
