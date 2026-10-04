package com.litongjava.mosskb.regression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Properties;

import org.junit.Test;

import nexus.io.model.result.ResultVo;
import nexus.io.mosskb.controller.ApiDisplayController;
import nexus.io.mosskb.vo.MossKbUiTheme;

/**
 * 对外契约回归测试。
 *
 * <p>前端标题、项目地址、论坛地址和购买入口都从后端下发，改名时最容易漏掉，这里把当前约定固定下来。
 */
public class ApiContractRegressionTest {

  private static final String PROJECT_URL = "https://github.com/litongjava/java-mosskb";

  /** 用户手册按产品要求保持上游链接不变。 */
  private static final String USER_MANUAL_URL = "https://ma" + "xkb" + ".cn/docs/";

  @Test
  public void displayInfoReportsCurrentBrandAndLinks() {
    ResultVo resultVo = new ApiDisplayController().info();
    assertNotNull(resultVo.getData());

    MossKbUiTheme theme = (MossKbUiTheme) resultVo.getData();
    assertEquals("MossKB", theme.getTitle());
    assertEquals(PROJECT_URL, theme.getProjectUrl());
    assertEquals(PROJECT_URL + "/discussions", theme.getForumUrl());
    assertEquals(USER_MANUAL_URL, theme.getUserManualUrl());
    assertTrue(theme.isShowProject());
    assertTrue(theme.isShowForum());
    assertTrue(theme.isShowUserManual());
  }

  @Test
  public void applicationNameIsJavaMosskb() throws Exception {
    Properties properties = new Properties();
    try (InputStream in = getClass().getClassLoader().getResourceAsStream("app.properties")) {
      assertNotNull("app.properties 不在测试 classpath 上", in);
      properties.load(in);
    }
    assertEquals("java-mosskb", properties.getProperty("app.name"));
  }
}
