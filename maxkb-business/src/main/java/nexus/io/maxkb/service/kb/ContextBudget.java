package nexus.io.maxkb.service.kb;

import nexus.io.maxkb.utils.TokenCounter;
import nexus.io.tio.utils.environment.EnvUtils;

public final class ContextBudget {
  private ContextBudget() { }

  public static int setting(String key, int fallback, int min, int max) {
    try {
      return Math.max(min, Math.min(max, Integer.parseInt(EnvUtils.get(key, Integer.toString(fallback)))));
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  public static int tokens(String text) {
    return TokenCounter.countTokens(text == null ? "" : text);
  }

  public static String clip(String text, int budget) {
    if (text == null) {
      return "";
    }
    if (tokens(text) <= budget) {
      return text;
    }
    int low = 0;
    int high = text.length();
    while (low < high) {
      int mid = (low + high + 1) / 2;
      if (tokens(text.substring(0, mid)) <= budget - 12) {
        low = mid;
      } else {
        high = mid - 1;
      }
    }
    if (low > 0 && Character.isHighSurrogate(text.charAt(low - 1))) {
      low--;
    }
    return text.substring(0, low) + "\n[内容按上下文预算截断]";
  }
}
