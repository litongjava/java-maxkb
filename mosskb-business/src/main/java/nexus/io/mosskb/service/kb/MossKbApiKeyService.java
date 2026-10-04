package nexus.io.mosskb.service.kb;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jfinal.kit.Kv;

import nexus.io.db.activerecord.Db;
import nexus.io.db.activerecord.Row;
import nexus.io.mosskb.constant.MossKbTableNames;
import nexus.io.mosskb.service.ApiKeyAuth;
import nexus.io.model.result.ResultVo;
import nexus.io.tio.utils.hutool.StrUtil;
import nexus.io.tio.utils.snowflake.SnowflakeIdUtils;

/**
 * API 密钥管理。密钥分为两级：应用密钥只对某个应用的问答接口有效，账号密钥代表账号本身。
 *
 * <p>每条 SQL 都写成 Java 文本块常量，参数全部走占位符；应用密钥的每次读写都带上
 * {@code application_id} 条件，账号密钥的每次读写都带上 {@code user_id} 条件，越权请求在
 * 数据库层就查不到记录。
 */
public class MossKbApiKeyService {

  /** 密钥列表最多返回的条数，避免密钥过多时把界面拖慢。 */
  private static final int KEY_LIMIT = 100;

  /** 单个密钥允许配置的来源数量上限。 */
  private static final int ORIGIN_LIMIT = 32;

  private static final String LIST_APPLICATION_KEYS = """
      select id, secret_key, application_id, user_id, is_active, allow_cross_domain, cross_domain_list, create_time
      from moss_kb_application_api_key
      where application_id = ?
        and deleted = 0
      order by create_time desc
      limit %d
      """.formatted(KEY_LIMIT);

  private static final String LIST_USER_KEYS = """
      select id, secret_key, user_id, is_active, allow_cross_domain, cross_domain_list, create_time
      from moss_kb_user_api_key
      where user_id = ?
        and deleted = 0
      order by create_time desc
      limit %d
      """.formatted(KEY_LIMIT);

  private static final String FIND_APPLICATION_KEY = """
      select id, secret_key, application_id, user_id, is_active, allow_cross_domain, cross_domain_list, create_time
      from moss_kb_application_api_key
      where id = ?
        and application_id = ?
        and deleted = 0
      """;

  private static final String FIND_USER_KEY = """
      select id, secret_key, user_id, is_active, allow_cross_domain, cross_domain_list, create_time
      from moss_kb_user_api_key
      where id = ?
        and user_id = ?
        and deleted = 0
      """;

  private static final String UPDATE_APPLICATION_KEY = """
      update moss_kb_application_api_key
      set is_active = ?,
          allow_cross_domain = ?,
          cross_domain_list = ?,
          update_time = now()
      where id = ?
        and application_id = ?
        and deleted = 0
      """;

  private static final String UPDATE_USER_KEY = """
      update moss_kb_user_api_key
      set is_active = ?,
          allow_cross_domain = ?,
          cross_domain_list = ?,
          update_time = now()
      where id = ?
        and user_id = ?
        and deleted = 0
      """;

  private static final String DELETE_APPLICATION_KEY = """
      update moss_kb_application_api_key
      set deleted = 1,
          is_active = false,
          update_time = now()
      where id = ?
        and application_id = ?
        and deleted = 0
      """;

  private static final String DELETE_USER_KEY = """
      update moss_kb_user_api_key
      set deleted = 1,
          is_active = false,
          update_time = now()
      where id = ?
        and user_id = ?
        and deleted = 0
      """;

  public ResultVo listApplicationKeys(Long userId, Long applicationId) {
    if (!ApplicationAccess.owns(userId, applicationId)) {
      return ResultVo.fail("应用不存在或无权访问");
    }
    return ResultVo.ok(toKv(Db.find(LIST_APPLICATION_KEYS, applicationId)));
  }

  public ResultVo createApplicationKey(Long userId, Long applicationId) {
    if (!ApplicationAccess.owns(userId, applicationId)) {
      return ResultVo.fail("应用不存在或无权访问");
    }
    Long apiKeyId = insert(MossKbTableNames.moss_kb_application_api_key, ApiKeyAuth.APPLICATION_PREFIX, userId,
        applicationId);
    return ResultVo.ok(Db.findFirst(FIND_APPLICATION_KEY, apiKeyId, applicationId).toKv());
  }

  public ResultVo updateApplicationKey(Long userId, Long applicationId, Long apiKeyId, JSONObject input) {
    if (!ApplicationAccess.owns(userId, applicationId)) {
      return ResultVo.fail("应用不存在或无权访问");
    }
    Row existing = Db.findFirst(FIND_APPLICATION_KEY, apiKeyId, applicationId);
    if (existing == null) {
      return ResultVo.fail("密钥不存在");
    }
    int updated = Db.update(UPDATE_APPLICATION_KEY, enabled(input, existing), crossDomain(input, existing),
        (Object) crossDomainList(input, existing), apiKeyId, applicationId);
    return updated == 1 ? ResultVo.ok() : ResultVo.fail("密钥不存在");
  }

  public ResultVo deleteApplicationKey(Long userId, Long applicationId, Long apiKeyId) {
    if (!ApplicationAccess.owns(userId, applicationId)) {
      return ResultVo.fail("应用不存在或无权访问");
    }
    int updated = Db.update(DELETE_APPLICATION_KEY, apiKeyId, applicationId);
    return updated == 1 ? ResultVo.ok() : ResultVo.fail("密钥不存在");
  }

  public ResultVo listUserKeys(Long userId) {
    return ResultVo.ok(toKv(Db.find(LIST_USER_KEYS, userId)));
  }

  public ResultVo createUserKey(Long userId) {
    Long apiKeyId = insert(MossKbTableNames.moss_kb_user_api_key, ApiKeyAuth.USER_PREFIX, userId, null);
    return ResultVo.ok(Db.findFirst(FIND_USER_KEY, apiKeyId, userId).toKv());
  }

  public ResultVo updateUserKey(Long userId, Long apiKeyId, JSONObject input) {
    Row existing = Db.findFirst(FIND_USER_KEY, apiKeyId, userId);
    if (existing == null) {
      return ResultVo.fail("密钥不存在");
    }
    int updated = Db.update(UPDATE_USER_KEY, enabled(input, existing), crossDomain(input, existing),
        (Object) crossDomainList(input, existing), apiKeyId, userId);
    return updated == 1 ? ResultVo.ok() : ResultVo.fail("密钥不存在");
  }

  public ResultVo deleteUserKey(Long userId, Long apiKeyId) {
    int updated = Db.update(DELETE_USER_KEY, apiKeyId, userId);
    return updated == 1 ? ResultVo.ok() : ResultVo.fail("密钥不存在");
  }

  /**
   * 新密钥默认启用、默认不允许跨域，来源列表为空；返回主键供调用方回查，保证新增与列表返回同一份结构。
   */
  private Long insert(String tableName, String prefix, Long userId, Long applicationId) {
    Long apiKeyId = SnowflakeIdUtils.id();
    Row record = Row.by("id", apiKeyId).set("secret_key", prefix + UUID.randomUUID().toString().replace("-", ""))
        .set("user_id", userId).set("is_active", true).set("allow_cross_domain", false)
        .set("cross_domain_list", new String[] {});
    if (applicationId != null) {
      record.set("application_id", applicationId);
    }
    Db.save(tableName, record);
    return apiKeyId;
  }

  private boolean enabled(JSONObject input, Row existing) {
    if (input.containsKey("is_active")) {
      return input.getBooleanValue("is_active");
    }
    return Boolean.TRUE.equals(existing.getBoolean("is_active"));
  }

  private boolean crossDomain(JSONObject input, Row existing) {
    if (input.containsKey("allow_cross_domain")) {
      return input.getBooleanValue("allow_cross_domain");
    }
    return Boolean.TRUE.equals(existing.getBoolean("allow_cross_domain"));
  }

  private String[] crossDomainList(JSONObject input, Row existing) {
    if (input.containsKey("cross_domain_list")) {
      return origins(input.getJSONArray("cross_domain_list"));
    }
    String[] current = existing.get("cross_domain_list");
    return current == null ? new String[] {} : current;
  }

  /** 来源列表做去空、去空白和长度限制，避免把任意文本写进数据库。 */
  private String[] origins(JSONArray array) {
    List<String> origins = new ArrayList<>();
    if (array != null) {
      for (int i = 0; i < array.size() && origins.size() < ORIGIN_LIMIT; i++) {
        String origin = array.getString(i);
        if (StrUtil.isNotBlank(origin) && origin.trim().length() <= 128) {
          origins.add(origin.trim());
        }
      }
    }
    return origins.toArray(new String[0]);
  }

  private List<Kv> toKv(List<Row> records) {
    List<Kv> kvs = new ArrayList<>();
    for (Row record : records) {
      kvs.add(record.toKv());
    }
    return kvs;
  }
}
