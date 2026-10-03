package nexus.io.maxkb.service.kb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class ChatLogCleanupServiceTest {

  private static class Record {
    long id;
    long chatId;
    long ageDays;

    Record(long id, long chatId, long ageDays) {
      this.id = id;
      this.chatId = chatId;
      this.ageDays = ageDays;
    }
  }

  private static class Store extends ChatLogCleanupService {
    final OffsetDateTime reference = OffsetDateTime.parse("2026-06-01T00:00:00Z");
    final Map<Long, Integer> applications = new LinkedHashMap<>();
    final Map<Long, Long> chatApplication = new LinkedHashMap<>();
    final List<Record> records = new ArrayList<>();
    final Set<Long> deletedChats = new LinkedHashSet<>();
    int deleteRecordCalls;

    @Override
    protected OffsetDateTime cutoff(Application application) {
      return reference.minusDays(application.retainDays());
    }

    @Override
    protected List<Application> applications() {
      List<Application> result = new ArrayList<>();
      applications.forEach((id, cleanTime) -> result.add(new Application(id, cleanTime)));
      return result;
    }

    @Override
    protected List<Long> expiredChatIds(Long applicationId, OffsetDateTime cutoff) {
      Set<Long> result = new LinkedHashSet<>();
      for (Record record : records) {
        if (applicationId.equals(chatApplication.get(record.chatId)) && reference.minusDays(record.ageDays).isBefore(cutoff)) {
          result.add(record.chatId);
        }
      }
      return new ArrayList<>(result);
    }

    @Override
    protected int deleteExpiredRecords(Long applicationId, OffsetDateTime cutoff) {
      deleteRecordCalls++;
      List<Record> expired = new ArrayList<>();
      for (Record record : records) {
        if (applicationId.equals(chatApplication.get(record.chatId)) && reference.minusDays(record.ageDays).isBefore(cutoff)) {
          expired.add(record);
        }
      }
      records.removeAll(expired);
      return expired.size();
    }

    @Override
    protected List<Long> chatsWithoutRecords(List<Long> chatIds) {
      List<Long> result = new ArrayList<>();
      for (Long chatId : chatIds) {
        boolean hasRecord = records.stream().anyMatch(record -> record.chatId == chatId);
        if (!hasRecord) {
          result.add(chatId);
        }
      }
      return result;
    }

    @Override
    protected int deleteChats(List<Long> chatIds) {
      deletedChats.addAll(chatIds);
      chatApplication.keySet().removeAll(chatIds);
      return chatIds.size();
    }
  }

  private Store store(long applicationId, Integer cleanTime) {
    Store store = new Store();
    store.applications.put(applicationId, cleanTime);
    return store;
  }

  @Test
  public void missingCleanTimeKeepsRecordsWithinDefaultRetention() {
    Store store = store(1L, null);
    store.chatApplication.put(10L, 1L);
    store.records.add(new Record(100L, 10L, 100));

    assertEquals(0, store.cleanExpired());
    assertEquals(1, store.records.size());
    assertEquals(0, store.deleteRecordCalls);
  }

  @Test
  public void expiredRecordsAreRemovedButChatWithRemainingRecordsIsKept() {
    Store store = store(1L, 30);
    store.chatApplication.put(10L, 1L);
    store.records.add(new Record(100L, 10L, 40));
    store.records.add(new Record(101L, 10L, 1));

    assertEquals(1, store.cleanExpired());
    assertEquals(1, store.records.size());
    assertEquals(101L, store.records.get(0).id);
    assertTrue(store.chatApplication.containsKey(10L));
    assertFalse(store.deletedChats.contains(10L));
  }

  @Test
  public void chatIsDeletedAfterItsLastRecordExpires() {
    Store store = store(2L, null);
    store.chatApplication.put(20L, 2L);
    store.records.add(new Record(200L, 20L, 200));

    assertEquals(1, store.cleanExpired());
    assertTrue(store.records.isEmpty());
    assertTrue(store.deletedChats.contains(20L));
  }

  @Test
  public void zeroCleanTimeFallsBackToDefaultRetention() {
    Store store = store(3L, 0);
    store.chatApplication.put(30L, 3L);
    store.records.add(new Record(300L, 30L, 200));
    store.records.add(new Record(301L, 30L, 100));

    assertEquals(1, store.cleanExpired());
    assertEquals(1, store.records.size());
    assertEquals(301L, store.records.get(0).id);
    assertTrue(store.deletedChats.contains(30L) == false);
  }

  @Test
  public void applicationsWithoutExpiredRecordsAreSkipped() {
    Store store = store(4L, 7);
    store.chatApplication.put(40L, 4L);
    store.records.add(new Record(400L, 40L, 3));

    assertEquals(0, store.cleanExpired());
    assertEquals(0, store.deleteRecordCalls);
    assertEquals(1, store.records.size());
  }

  @Test
  public void retentionDaysAreReadPerApplication() {
    Store store = new Store();
    store.applications.put(1L, 30);
    store.applications.put(2L, 365);
    store.chatApplication.put(10L, 1L);
    store.chatApplication.put(20L, 2L);
    store.records.add(new Record(100L, 10L, 60));
    store.records.add(new Record(200L, 20L, 60));

    assertEquals(1, store.cleanExpired());
    assertEquals(1, store.records.size());
    assertEquals(200L, store.records.get(0).id);
  }
}
