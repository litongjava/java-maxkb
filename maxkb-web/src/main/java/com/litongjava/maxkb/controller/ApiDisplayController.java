package com.litongjava.maxkb.controller;

import com.litongjava.annotation.RequestPath;
import com.litongjava.maxkb.vo.MaxKbUiTheme;
import com.litongjava.model.result.ResultVo;

@RequestPath("/api/display")
public class ApiDisplayController {

  public ResultVo info() {
    String title = "MaxKB";
    String slogan = "欢迎使用 MaxKB 智能知识库问答系统";
    String userManualUrl = "https://maxkb.cn/docs/";
    String forumUrl = "https://bbs.fit2cloud.com/c/mk/11";
    String projectUrl = "https://github.com/1Panel-dev/MaxKB";
    MaxKbUiTheme maxKbUiTheme = new MaxKbUiTheme("#3370FF", "", "", "", title, slogan, true, userManualUrl, true,
        forumUrl, true, projectUrl);
    return ResultVo.ok(maxKbUiTheme);
  }
}
