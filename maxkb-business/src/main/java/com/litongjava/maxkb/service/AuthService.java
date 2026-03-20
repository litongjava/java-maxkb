package com.litongjava.maxkb.service;

import com.litongjava.tio.boot.admin.services.TioBootAdminTokenPredicate;
import com.litongjava.tio.boot.admin.utils.TioAdminEnvUtils;
import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.utils.token.TokenManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AuthService {
  private TioBootAdminTokenPredicate tioBootAdminTokenPredicate = new TioBootAdminTokenPredicate();

  /**
   * @param authorization
   * @return
   */
  public Long getIdByToken(String authorization) {
    log.info("authorization:{}", authorization);
    if (StrUtil.isBlank(authorization)) {
      return null;
    }

    String[] split = authorization.split(" ");

    String SECRET_KEY = TioAdminEnvUtils.getAdminSecretKey();
    String userId;
    String idToken = null;
    if (split.length > 1) {
      idToken = split[1];
    } else {
      idToken = authorization;
    }

    boolean test = tioBootAdminTokenPredicate.test(idToken);
    if (test) {
      userId = tioBootAdminTokenPredicate.parseUserIdString(idToken);
    } else {
      userId = TokenManager.parseUserIdString(SECRET_KEY, idToken);
    }
    return Long.valueOf(userId);
  }

}
