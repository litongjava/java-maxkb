package nexus.io.mosskb.service.kb;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** One in-flight turn per conversation; released only after the answer is persisted. */
public final class ChatExecution {
  private static final Set<Long> ACTIVE = ConcurrentHashMap.newKeySet();

  private ChatExecution() { }

  public static boolean begin(Long chatId) {
    return ACTIVE.add(chatId);
  }

  public static void end(Long chatId) {
    ACTIVE.remove(chatId);
  }
}
