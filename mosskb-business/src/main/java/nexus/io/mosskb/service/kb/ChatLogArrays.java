package nexus.io.mosskb.service.kb;

import java.sql.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * 标注段落 ID 列（BIGINT[]）在不同驱动返回类型下的统一转换。
 *
 * JDBC 直接查询返回 java.sql.Array，测试或中间层可能给出数组、集合或 PostgreSQL 字面量，
 * 界面需要的是数组，统一在这里收口，避免把 java.sql.Array 直接写进 JSON。
 */
final class ChatLogArrays {
  private ChatLogArrays() {
  }

  static List<Long> toLongList(Object value) {
    List<Long> result = new ArrayList<>();
    if (value == null) {
      return result;
    }
    if (value instanceof Array array) {
      try {
        return toLongList(array.getArray());
      } catch (java.sql.SQLException e) {
        throw new IllegalStateException("读取标注段落列表失败", e);
      }
    }
    if (value instanceof Object[] items) {
      for (Object item : items) {
        add(result, item);
      }
      return result;
    }
    if (value instanceof Iterable<?> items) {
      for (Object item : items) {
        add(result, item);
      }
      return result;
    }
    // PostgreSQL 数组字面量：{1,2} 或 [1,2]
    String text = value instanceof org.postgresql.util.PGobject pgobject ? pgobject.getValue() : value.toString();
    if (text == null) {
      return result;
    }
    text = text.trim();
    if (text.startsWith("{") && text.endsWith("}")) {
      text = text.substring(1, text.length() - 1);
    } else if (text.startsWith("[") && text.endsWith("]")) {
      text = text.substring(1, text.length() - 1);
    }
    for (String part : text.split(",")) {
      if (!part.isBlank()) {
        add(result, part.trim().replace("\"", ""));
      }
    }
    return result;
  }

  private static void add(List<Long> result, Object item) {
    if (item == null) {
      return;
    }
    if (item instanceof Number number) {
      result.add(number.longValue());
      return;
    }
    String text = item.toString().trim();
    if (text.isEmpty() || "null".equals(text)) {
      return;
    }
    result.add(Long.valueOf(text));
  }
}
