package nexus.io.maxkb.service.kb;

import java.util.*;
import com.alibaba.fastjson2.JSONObject;
import nexus.io.maxkb.vo.ParagraphSearchResultVo;
import org.junit.Test;
import static org.junit.Assert.*;

public class IterativeRetrievalServiceTest {
  private static class Agent extends IterativeRetrievalService {
    int calls;
    List<JSONObject> decisions = new ArrayList<>();
    List<List<ParagraphSearchResultVo>> found = new ArrayList<>();
    @Override protected String rewrite(List<JSONObject> history, String q) { return q; }
    @Override protected List<ParagraphSearchResultVo> search(Long[] ids, Float threshold, Integer limit, String q, String mode) {
      return found.get(Math.min(calls++, found.size() - 1));
    }
    @Override protected JSONObject assess(String q, List<JSONObject> h, List<ParagraphSearchResultVo> p, Set<String> s) {
      return decisions.remove(0);
    }
  }
  private ParagraphSearchResultVo evidence(long id) { return new ParagraphSearchResultVo(id, "资料" + id, "来源", 1); }
  private JSONObject decision(boolean enough, String... next) {
    return JSONObject.of("sufficient", enough, "missing", enough ? "" : "缺少第二项", "queries", List.of(next));
  }
  @Test public void missingEvidenceTriggersAnotherRetrievalAndDeduplicatesSources() {
    Agent a = new Agent();
    a.found.add(List.of(evidence(1))); a.found.add(List.of(evidence(1), evidence(2)));
    a.decisions.add(decision(false, "第二项")); a.decisions.add(decision(true));
    var result = a.retrieve(new Long[]{1L}, 0f, 5, "blend", "问题", List.of(), e -> { });
    assertEquals(2, a.calls); assertEquals(2, result.getParagraph_list().size());
    assertEquals("sufficient", result.getStop_reason()); assertTrue(result.getSufficient());
    assertEquals(2, result.getIterations().size());
  }
  @Test public void repeatedQueryStopsWithoutRepeatedRequest() {
    Agent a = new Agent(); a.found.add(List.of(evidence(1))); a.decisions.add(decision(false, "问题？"));
    var result = a.retrieve(new Long[]{1L}, 0f, 5, "embedding", "问题", List.of(), e -> { });
    assertEquals(1, a.calls); assertEquals("duplicate_query", result.getStop_reason());
  }
  @Test public void noNewEvidenceStopsUnproductiveLoop() {
    Agent a = new Agent(); a.found.add(List.of(evidence(1)));
    a.decisions.add(decision(false, "另一个问题")); a.decisions.add(decision(false, "第三个问题"));
    var result = a.retrieve(new Long[]{1L}, 0f, 5, "embedding", "问题", List.of(), e -> { });
    assertEquals(2, a.calls); assertEquals("no_new_evidence", result.getStop_reason());
  }
  @Test public void modelCannotDeclareEmptyEvidenceSufficient() {
    Agent a = new Agent(); a.found.add(List.of()); a.decisions.add(decision(true));
    var result = a.retrieve(new Long[]{1L}, 0f, 5, "embedding", "问题", List.of(), e -> { });
    assertFalse(result.getSufficient()); assertEquals("no_followup_query", result.getStop_reason());
  }
  @Test public void brokenAssessmentReturnsExplicitUncertainty() {
    Agent a = new Agent(); a.found.add(List.of(evidence(1))); a.decisions.add(JSONObject.of("sufficient", "yes"));
    var result = a.retrieve(new Long[]{1L}, 0f, 5, "embedding", "问题", List.of(), e -> { });
    assertEquals("assessment_error", result.getStop_reason()); assertFalse(result.getSufficient());
  }
  @Test public void roundLimitTerminatesEvenWhenEverySearchFindsSomething() {
    Agent a = new Agent();
    for (int i = 0; i < 6; i++) {
      a.found.add(List.of(evidence(i))); a.decisions.add(decision(false, "补充" + i));
    }
    var result = a.retrieve(new Long[]{1L}, 0f, 5, "embedding", "问题", List.of(), e -> { });
    assertEquals(3, a.calls); assertEquals("max_rounds", result.getStop_reason());
  }
  @Test public void noDatasetMakesNoModelAssessmentOrSearch() {
    Agent a = new Agent();
    var result = a.retrieve(new Long[]{}, 0f, 5, "embedding", "问题", List.of(), e -> { });
    assertEquals(0, a.calls); assertEquals("no_datasets", result.getStop_reason());
  }
}
