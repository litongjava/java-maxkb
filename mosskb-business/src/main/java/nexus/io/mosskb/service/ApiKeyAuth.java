package nexus.io.mosskb.service;

import nexus.io.db.activerecord.Db;
import nexus.io.db.activerecord.Row;

/**
 * API 密钥解析。密钥是不透明字符串而不是 JWT，因此不能走令牌校验，需要直接在密钥表里查找。
 *
 * <p>应用密钥以 {@code application-} 开头，只允许访问所属应用的问答接口；账号密钥以
 * {@code system-} 开头，代表密钥所属账号本身。
 */
public final class ApiKeyAuth {

  /** 应用密钥前缀。 */
  public static final String APPLICATION_PREFIX = "application-";

  /** 账号密钥前缀。 */
  public static final String USER_PREFIX = "system-";

  private static final String FIND_APPLICATION_KEY = """
      select k.id, k.application_id, k.user_id, k.is_active, k.allow_cross_domain, k.cross_domain_list
      from moss_kb_application_api_key k
      join moss_kb_application a on a.id = k.application_id
      where k.secret_key = ?
        and k.deleted = 0
        and a.deleted = 0
      """;

  private static final String FIND_USER_KEY = """
      select id, user_id, is_active, allow_cross_domain, cross_domain_list
      from moss_kb_user_api_key
      where secret_key = ?
        and deleted = 0
      """;

  private ApiKeyAuth() {
  }

  /** 判断一个 Bearer 值是不是 API 密钥。 */
  public static boolean isApiKey(String token) {
    return token != null && (token.startsWith(APPLICATION_PREFIX) || token.startsWith(USER_PREFIX));
  }

  /** 按密钥查找主体；密钥不存在或已逻辑删除时返回 null。 */
  public static ApiKeyPrincipal resolve(String token) {
    if (!isApiKey(token)) {
      return null;
    }
    boolean application = token.startsWith(APPLICATION_PREFIX);
    Row record = Db.findFirst(application ? FIND_APPLICATION_KEY : FIND_USER_KEY, token);
    if (record == null) {
      return null;
    }
    String[] crossDomainList = record.get("cross_domain_list");
    if (crossDomainList == null) {
      crossDomainList = new String[] {};
    }
    return new ApiKeyPrincipal(record.getLong("user_id"), application ? record.getLong("application_id") : null,
        Boolean.TRUE.equals(record.getBoolean("is_active")), Boolean.TRUE.equals(record.getBoolean("allow_cross_domain")),
        crossDomainList);
  }

  /**
   * 密钥主体。
   *
   * @param userId            密钥所属账号，请求上下文按该账号建立
   * @param applicationId     应用密钥所属应用；账号密钥为 null
   * @param active            密钥是否启用
   * @param allowCrossDomain  是否允许跨域调用
   * @param crossDomainList   允许的来源列表，空列表表示不限制来源
   */
  public record ApiKeyPrincipal(Long userId, Long applicationId, boolean active, boolean allowCrossDomain,
      String[] crossDomainList) {

    public boolean applicationKey() {
      return applicationId != null;
    }
  }
}
