package nexus.io.maxkb.service.kb;

import nexus.io.db.activerecord.Db;
import nexus.io.db.activerecord.Row;

public final class ApplicationAccess {
  private ApplicationAccess() {}

  public static boolean owns(Long userId, Long applicationId) {
    if (userId == null || applicationId == null) {
      return false;
    }
    Row app = Db.findById("max_kb_application", applicationId);
    return app != null && (userId == 1L || userId.equals(app.getLong("user_id")));
  }

  public static boolean canChat(Long clientId, Long applicationId) {
    if (owns(clientId, applicationId)) {
      return true;
    }
    return clientId != null && Db.queryLong("select count(*) from max_kb_application_public_access_client c join max_kb_application_access_token t on t.application_id=c.application_id where c.client_id=? and c.application_id=? and t.is_active=true and t.deleted=0", clientId, applicationId) > 0;
  }

  public static boolean canReadChat(Long clientId, Long applicationId, Long chatId) {
    Row chat = Db.findById("max_kb_application_chat", chatId);
    return chat != null && !Boolean.TRUE.equals(chat.getBoolean("is_deleted")) && applicationId.equals(chat.getLong("application_id"))
        && (owns(clientId, applicationId) || (canChat(clientId, applicationId) && clientId.equals(chat.getLong("client_id"))));
  }
}
