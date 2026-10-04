package nexus.io.mosskb.service.kb;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import nexus.io.db.activerecord.Db;
import nexus.io.db.activerecord.Row;

/**
 * 对话日志清理策略：按应用的保留天数删除过期问答。
 *
 * 保留天数取 moss_kb_application.clean_time，未设置或非正数时按 180 天处理（与官方一致）。
 * 只删除过期的问答记录；因此变空的会话随后单独删除，仍有记录的会话保留。
 * 数据库访问集中在可覆盖的方法里，便于用内存数据做单元测试。
 */
public class ChatLogCleanupService {

  public static final int DEFAULT_RETAIN_DAYS = 180;

  public record Application(Long id, Integer cleanTime) {
    public int retainDays() {
      return cleanTime == null || cleanTime <= 0 ? DEFAULT_RETAIN_DAYS : cleanTime;
    }
  }

  /** 返回删除的问答记录条数。 */
  public int cleanExpired() {
    int deleted = 0;
    for (Application application : applications()) {
      OffsetDateTime cutoff = cutoff(application);
      List<Long> chatIds = expiredChatIds(application.id(), cutoff);
      if (chatIds.isEmpty()) {
        continue;
      }
      deleted += deleteExpiredRecords(application.id(), cutoff);
      List<Long> emptyChats = chatsWithoutRecords(chatIds);
      if (!emptyChats.isEmpty()) {
        deleteChats(emptyChats);
      }
    }
    return deleted;
  }

  protected OffsetDateTime cutoff(Application application) {
    return OffsetDateTime.now().minusDays(application.retainDays());
  }

  protected List<Application> applications() {
    List<Application> result = new ArrayList<>();
    for (Row row : Db.find("select id,clean_time from moss_kb_application")) {
      result.add(new Application(row.getLong("id"), row.getInt("clean_time")));
    }
    return result;
  }

  protected List<Long> expiredChatIds(Long applicationId, OffsetDateTime cutoff) {
    return Db.queryListLong("select distinct r.chat_id from moss_kb_application_chat_record r"
        + " join moss_kb_application_chat c on c.id=r.chat_id"
        + " where c.application_id=? and r.create_time < ?", applicationId, cutoff);
  }

  protected int deleteExpiredRecords(Long applicationId, OffsetDateTime cutoff) {
    return Db.update("delete from moss_kb_application_chat_record r using moss_kb_application_chat c"
        + " where r.chat_id=c.id and c.application_id=? and r.create_time < ?", applicationId, cutoff);
  }

  protected List<Long> chatsWithoutRecords(List<Long> chatIds) {
    StringBuilder placeholders = new StringBuilder("(");
    for (int i = 0; i < chatIds.size(); i++) {
      placeholders.append(i == 0 ? "?" : ",?");
    }
    placeholders.append(")");
    return Db.queryListLong("select c.id from moss_kb_application_chat c where c.id in " + placeholders
        + " and not exists (select 1 from moss_kb_application_chat_record r where r.chat_id=c.id)", chatIds.toArray());
  }

  protected int deleteChats(List<Long> chatIds) {
    StringBuilder placeholders = new StringBuilder("(");
    for (int i = 0; i < chatIds.size(); i++) {
      placeholders.append(i == 0 ? "?" : ",?");
    }
    placeholders.append(")");
    return Db.update("delete from moss_kb_application_chat where id in " + placeholders, chatIds.toArray());
  }
}
