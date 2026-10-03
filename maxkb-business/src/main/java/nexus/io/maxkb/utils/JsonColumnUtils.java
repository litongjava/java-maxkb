package nexus.io.maxkb.utils;

import org.postgresql.util.PGobject;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

/**
 * jsonb 列在部分查询路径下会以 PGobject 返回，直接序列化会得到
 * {"type":"jsonb","value":"..."} 这样的结构。对外返回前统一转换一次。
 */
public final class JsonColumnUtils {

  private JsonColumnUtils() {
  }

  @SuppressWarnings("unchecked")
  public static JSONObject toJsonObject(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof PGobject pgObject) {
      return parse(pgObject.getValue());
    }
    if (value instanceof JSONObject object) {
      return object;
    }
    if (value instanceof java.util.Map<?, ?> map) {
      return new JSONObject((java.util.Map<String, Object>) map);
    }
    if (value instanceof String text) {
      return parse(text);
    }
    return null;
  }

  private static JSONObject parse(String text) {
    if (text == null || text.isBlank()) {
      return null;
    }
    return JSON.parseObject(text);
  }
}
