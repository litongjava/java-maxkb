package com.litongjava.maxkb.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaxKbUiTheme {
  private String theme;
  private String icon;
  private String loginLogo;
  private String loginImage;
  private String title;
  private String slogan;
  private boolean showUserManual;
  private String userManualUrl;
  private boolean showForum;
  private String forumUrl;
  private boolean showProject;
  private String projectUrl;
}
