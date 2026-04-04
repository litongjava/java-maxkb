package nexus.io.maxkb.service;

import lombok.extern.slf4j.Slf4j;
import nexus.io.tio.boot.admin.services.TioBootAdminTokenPredicate;
import nexus.io.tio.boot.admin.utils.TioAdminEnvUtils;
import nexus.io.tio.boot.token.PredicateResult;
import nexus.io.tio.utils.hutool.StrUtil;
import nexus.io.tio.utils.token.TokenManager;

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
