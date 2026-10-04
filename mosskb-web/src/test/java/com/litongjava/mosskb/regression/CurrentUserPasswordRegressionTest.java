package com.litongjava.mosskb.regression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.alibaba.fastjson2.JSONObject;

import nexus.io.db.activerecord.Db;
import nexus.io.model.result.ResultVo;
import nexus.io.mosskb.service.KbUserService;
import nexus.io.mosskb.service.UserPassword;
import nexus.io.tio.utils.snowflake.SnowflakeIdUtils;

/**
 * 修改当前用户密码的回归测试。
 *
 * <p>产品要求改密不再验证邮箱：请求体里只有 password 与 re_password，身份完全来自登录令牌。
 * 这里固定住三件事——不带验证码可以改密、两次密码不一致时拒绝、改密后 token_version 递增使旧令牌失效。
 *
 * <p>测试用独立的账号，绝不动种子里的 admin：admin 的初始密码被另一组回归测试断言着。
 */
public class CurrentUserPasswordRegressionTest {

  private static final String USERNAME = "pwd_regression_user";
  private static final String OLD_PASSWORD = "old-password-123";
  private static final String NEW_PASSWORD = "new-password-456";

  private static final String SELECT_PASSWORD = """
      select password
      from moss_kb_user
      where id = ?
      """;

  private static final String SELECT_TOKEN_VERSION = """
      select token_version
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
  public void passwordChangesWithoutEmailCodeAndInvalidatesOldToken() {
    long id = createUser();
    long versionBefore = Db.queryLong(SELECT_TOKEN_VERSION, id);

    ResultVo result = new KbUserService().resetCurrentPassword(id,
        JSONObject.of("password", NEW_PASSWORD, "re_password", NEW_PASSWORD));

    assertEquals(200, result.getCode());
    String stored = Db.queryStr(SELECT_PASSWORD, id);
    assertTrue("新密码无法通过校验", UserPassword.matches(NEW_PASSWORD, stored));
    assertFalse("旧密码仍然可以登录", UserPassword.matches(OLD_PASSWORD, stored));
    assertEquals("改密后必须递增 token_version，否则旧令牌还能用", Long.valueOf(versionBefore + 1),
        Db.queryLong(SELECT_TOKEN_VERSION, id));
  }

  @Test
  public void mismatchOrTooShortPasswordIsRejected() {
    long id = createUser();

    ResultVo mismatch = new KbUserService().resetCurrentPassword(id,
        JSONObject.of("password", NEW_PASSWORD, "re_password", NEW_PASSWORD + "x"));
    assertEquals(400, mismatch.getCode());

    ResultVo tooShort = new KbUserService().resetCurrentPassword(id,
        JSONObject.of("password", "12345", "re_password", "12345"));
    assertEquals(400, tooShort.getCode());

    assertTrue("被拒绝的请求不应该改掉密码", UserPassword.matches(OLD_PASSWORD, Db.queryStr(SELECT_PASSWORD, id)));
  }

  private long createUser() {
    long id = SnowflakeIdUtils.id();
    Db.update("""
        insert into moss_kb_user
          (id, email, phone, nick_name, username, password, role, is_active, source, deleted, token_version)
        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, id, "", "", "", USERNAME, UserPassword.hash(OLD_PASSWORD), "USER", true, "LOCAL", 0, 0);
    userId = id;
    return id;
  }
}
