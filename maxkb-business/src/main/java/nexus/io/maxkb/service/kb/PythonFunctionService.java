package nexus.io.maxkb.service.kb;

import java.util.List;
import java.util.ArrayList;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import nexus.io.db.activerecord.Db;
import nexus.io.db.activerecord.Row;
import nexus.io.jfinal.aop.Aop;
import nexus.io.model.result.ResultVo;
import nexus.io.tio.utils.snowflake.SnowflakeIdUtils;

public class PythonFunctionService {
  public ResultVo list(Long user, Integer page, Integer size, String name) {
    int limit = Math.max(1, Math.min(100, size == null ? 100 : size));
    int current = Math.max(1, page == null ? 1 : page);
    String where = " from max_kb_function_lib where deleted=false and (user_id=? or permission_type='PUBLIC') and name ilike ?";
    String pattern = "%" + (name == null ? "" : name) + "%";
    List<JSONObject> records = new ArrayList<>();
    for (Row row : Db.find("select *" + where + " order by id desc limit ? offset ?", user, pattern, limit, (current - 1) * limit)) {
      records.add(value(row, user));
    }
    return ResultVo.ok(page == null ? records : JSONObject.of("current", current, "size", limit,
        "total", Db.queryLong("select count(*)" + where, user, pattern), "records", records));
  }

  public ResultVo get(Long user, Long id) {
    Row row = Db.findFirst("select * from max_kb_function_lib where id=? and deleted=false and (user_id=? or permission_type='PUBLIC')", id, user);
    return row == null ? ResultVo.fail("函数不存在或无权访问") : ResultVo.ok(value(row, user));
  }

  public ResultVo save(Long user, Long id, JSONObject input) {
    JSONObject data = input;
    if (id != null) {
      Row existing = owned(user, id);
      if (existing == null) {
        return ResultVo.fail("函数不存在或无权修改");
      }
      data = value(existing, user);
      data.putAll(input);
    }
    String name = data.getString("name");
    String code = data.getString("code");
    if (name == null || name.isBlank() || name.length() > 128 || code == null || code.isBlank() || code.length() > 65536) {
      return ResultVo.fail("函数名称或代码格式无效");
    }
    String permission = data.getString("permission_type");
    if (permission == null) {
      permission = "PRIVATE";
    }
    if (!List.of("PRIVATE", "PUBLIC").contains(permission)) {
      return ResultVo.fail("无效的函数权限类型");
    }
    long savedId = id == null ? SnowflakeIdUtils.id() : id;
    String fields = JSON.toJSONString(data.getOrDefault("input_field_list", new JSONArray()));
    String initFields = JSON.toJSONString(data.getOrDefault("init_field_list", new JSONArray()));
    String init = JSON.toJSONString(data.getOrDefault("init_params", new JSONObject()));
    boolean active = !Boolean.FALSE.equals(data.getBoolean("is_active"));
    if (id == null) {
      Db.update("insert into max_kb_function_lib(id,user_id,name,\"desc\",code,input_field_list,init_field_list,init_params,permission_type,is_active) values(?,?,?,?,?,?::jsonb,?::jsonb,?::jsonb,?,?)",
          savedId, user, name, data.getOrDefault("desc", ""), code, fields, initFields, init, permission, active);
    } else {
      Db.update("update max_kb_function_lib set name=?,\"desc\"=?,code=?,input_field_list=?::jsonb,init_field_list=?::jsonb,init_params=?::jsonb,permission_type=?,is_active=?,update_time=now() where id=? and user_id=?",
          name, data.getOrDefault("desc", ""), code, fields, initFields, init, permission, active, savedId, user);
    }
    return get(user, savedId);
  }

  public ResultVo remove(Long user, Long id) {
    return ResultVo.ok(Db.update("update max_kb_function_lib set deleted=true,update_time=now() where id=? and user_id=?", id, user) > 0);
  }

  public Object debug(JSONObject input) {
    JSONObject params = input.getJSONObject("init_params");
    params = params == null ? new JSONObject() : new JSONObject(params);
    JSONObject debug = new JSONObject();
    JSONArray values = input.getJSONArray("debug_field_list");
    if (values != null) {
      for (Object item : values) {
        JSONObject field = (JSONObject) item;
        debug.put(field.getString("name"), field.get("value"));
      }
    }
    JSONArray fields = input.getJSONArray("input_field_list");
    if (fields != null) {
      for (Object item : fields) {
        JSONObject field = (JSONObject) item;
        String name = field.getString("name");
        Object value = debug.get(name);
        if (value == null && field.getBooleanValue("is_required")) {
          throw new IllegalArgumentException("缺少函数参数：" + name);
        }
        params.put(name, convert(value, field.getString("type")));
      }
    }
    return Aop.get(IsolatedPythonExecutor.class).execute(input.getString("code"), params, input.getString("entrypoint"), false);
  }

  /** Shared entry point for Java workflow function nodes. */
  public Object execute(Long user, Long id, JSONObject arguments) {
    Row row = owned(user, id);
    if (row == null || !Boolean.TRUE.equals(row.getBoolean("is_active"))) {
      throw new IllegalArgumentException("函数不存在、已停用或无权执行");
    }
    JSONObject params = JSON.parseObject(java.util.Objects.toString(row.getObject("init_params")));
    if (arguments != null) {
      params.putAll(arguments);
    }
    return Aop.get(IsolatedPythonExecutor.class).execute(row.getStr("code"), params, null, false);
  }

  static Object convert(Object value, String type) {
    if (value == null) {
      return null;
    }
    return switch (type == null ? "string" : type) {
      case "int" -> Long.valueOf(value.toString());
      case "float" -> Double.valueOf(value.toString());
      case "dict" -> JSON.parseObject(value.toString());
      case "array" -> JSON.parseArray(value.toString());
      default -> value.toString();
    };
  }

  private Row owned(Long user, Long id) {
    return Db.findFirst("select * from max_kb_function_lib where id=? and user_id=? and deleted=false", id, user);
  }

  private JSONObject value(Row row, Long user) {
    JSONObject value = new JSONObject(row.getColumns());
    value.put("id", row.getLong("id").toString());
    value.put("user_id", row.getLong("user_id").toString());
    for (String key : List.of("input_field_list", "init_field_list", "init_params")) {
      value.put(key, JSON.parse(java.util.Objects.toString(row.getObject(key))));
    }
    if (!user.equals(row.getLong("user_id"))) {
      value.put("init_params", new JSONObject());
    }
    value.put("create_time", java.util.Objects.toString(row.getObject("create_time")));
    value.put("update_time", java.util.Objects.toString(row.getObject("update_time")));
    return value;
  }
}
