package nexus.io.maxkb.service.kb;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ConversationContextServiceTest {
  private static class Memory extends ConversationContextService {
    Snapshot state = new Snapshot("", 0, 0, 0);
    List<Turn> transcript = new ArrayList<>();
    int summaries;
    boolean fail;
    int budget = 6000;
    @Override protected int tokenBudget() { return budget; }
    @Override protected Snapshot read(Long chat) { return state; }
    @Override protected List<Turn> latest(Long chat, long after, long before, int limit) {
      List<Turn> matching = transcript.stream().filter(t -> t.id() > after && t.id() < before).toList();
      return new ArrayList<>(matching.subList(Math.max(0, matching.size() - limit), matching.size()));
    }
    @Override protected List<Turn> older(Long chat, long after, long before) {
      return transcript.stream().filter(t -> t.id() > after && t.id() < before).limit(20).toList();
    }
    @Override protected String summarize(String previous, List<Turn> turns) {
      summaries++;
      if (fail) { return ""; }
      return previous + "事实：" + turns.stream().map(Turn::question).toList();
    }
    @Override protected void save(Long chat, Snapshot old, Snapshot next) { state = next; }
  }
  private Memory history(int count) {
    Memory memory = new Memory();
    for (int i = 1; i <= count; i++) {
      memory.transcript.add(new ConversationContextService.Turn(i, "用户编号" + i, "回复" + i));
    }
    return memory;
  }
  @Test public void compactedMemoryAndRecentOriginalMessagesAreBothCarriedForward() {
    Memory m = history(8); m.budget = 1;
    var context = m.load(1L, 9, 3, false);
    assertTrue(m.state.through() > 0); assertTrue(m.state.rounds() > 0);
    assertTrue(context.messages().size() >= 3);
    assertTrue(context.messages().get(0).getString("content").contains("用户编号1"));
    assertTrue(context.metadata().getBooleanValue("compacted"));
    assertEquals(8, m.transcript.size());
  }
  @Test public void reloadDoesNotSummarizeSameRowsTwice() {
    Memory m = history(8); m.budget = 1; m.load(1L, 9, 3, false);
    int previous = m.summaries; int revision = m.state.revision();
    m.budget = 6000;
    m.load(1L, 9, 3, false);
    m.transcript.add(new ConversationContextService.Turn(9, "最新更正", "确认"));
    m.load(1L, 10, 3, false);
    assertEquals(previous, m.summaries); assertEquals(revision, m.state.revision());
  }
  @Test public void failedSummaryDoesNotAdvanceCheckpoint() {
    Memory m = history(8); m.budget = 1; m.fail = true;
    try { m.load(1L, 9, 3, false); fail("expected failure"); } catch (IllegalStateException expected) { }
    assertEquals(0, m.state.through()); assertEquals(8, m.transcript.size());
  }
  @Test public void currentAndFutureRecordsNeverLeakIntoContext() {
    Memory m = history(8); var result = m.load(1L, 4, 5, false);
    assertEquals(6, result.messages().size()); assertEquals(0, m.summaries);
    assertFalse(result.messages().toString().contains("用户编号4"));
  }
  @Test public void disablingHistoryAlsoDisablesExistingSummary() {
    Memory m = history(8); m.load(1L, 9, 3, false);
    assertTrue(m.load(1L, 9, 0, false).messages().isEmpty());
  }
  @Test public void smallContextNeverCompactsEvenForManualRequestsOrManyRounds() {
    Memory m = history(30);
    var result = m.load(1L, 31, 1, true);
    assertEquals(0, m.summaries); assertEquals(60, result.messages().size());
    assertFalse(result.metadata().getBooleanValue("threshold_exceeded"));
    assertEquals("用户编号1", result.messages().get(0).getString("content"));
  }
  @Test public void exactThresholdDoesNotCompactAndOneTokenOverDoes() {
    Memory m = history(8);
    int tokens = m.transcript.stream().mapToInt(t -> ContextBudget.tokens(t.question()) + ContextBudget.tokens(t.answer())).sum();
    m.budget = tokens;
    int[] progress = {0};
    m.load(1L, 9, 3, () -> { progress[0]++; });
    assertEquals(0, m.summaries); assertEquals(0, progress[0]);
    m.budget--;
    m.load(1L, 9, 3, () -> { progress[0]++; });
    assertTrue(m.summaries > 0); assertEquals(1, progress[0]);
    assertEquals(8, m.transcript.size());
  }
  @Test public void longSingleTurnDoesNotPretendToCompactWithoutOlderHistory() {
    Memory m = history(1); m.budget = 1;
    int[] progress = {0};
    var result = m.load(1L, 2, 5, () -> { progress[0]++; });
    assertEquals(0, m.summaries); assertEquals(0, progress[0]);
    assertFalse(result.metadata().getBooleanValue("compacted"));
  }
  @Test public void sameConversationCannotGenerateOrCompactConcurrently() {
    assertTrue(ChatExecution.begin(42L)); assertFalse(ChatExecution.begin(42L));
    ChatExecution.end(42L); assertTrue(ChatExecution.begin(42L)); ChatExecution.end(42L);
  }
}
