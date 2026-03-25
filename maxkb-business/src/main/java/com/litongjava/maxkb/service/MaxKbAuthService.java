package com.litongjava.maxkb.service;

import com.litongjava.tio.boot.admin.services.TioBootAdminTokenPredicate;
import com.litongjava.tio.boot.admin.utils.TioAdminEnvUtils;
import com.litongjava.tio.boot.token.PredicateResult;
import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.utils.token.TokenManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MaxKbAuthService {
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

    String idToken = null;
    if (split.length > 1) {
      idToken = split[1];
    } else {
      idToken = authorization;
    }

    String userId;
    PredicateResult validate = tioBootAdminTokenPredicate.validate(idToken);
    if (validate.isOk()) {
      userId = validate.getUserId();
      return Long.valueOf(userId);
    } else {
      String SECRET_KEY = TioAdminEnvUtils.getAdminSecretKey();
      userId = TokenManager.parseUserIdString(SECRET_KEY, idToken);
    }

    if (userId != null) {
      return Long.valueOf(userId);
    }
    return null;
  }

}
