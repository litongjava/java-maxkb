package nexus.io.maxkb.service.kb;

import java.util.List;
import com.alibaba.fastjson2.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class GiteeAuxiliaryModelTest {
  @Test public void auxiliaryCallsReserveOutputForTheActualDecision() {
    JSONObject body = (JSONObject) com.alibaba.fastjson2.JSON.toJSON(nexus.io.chat.UniChatClient.toOpenAiRequest(KnowledgeModelService.completionRequest("system", "input", 900, true)));
    assertEquals("disabled", body.getJSONObject("thinking").getString("type"));
    assertEquals("json_object", body.getJSONObject("response_format").getString("type"));
    assertEquals(900, body.getIntValue("max_tokens"));
  }
  @Test public void truncatedSummaryCannotBeCommittedAsValidContext() {
    JSONObject result = JSONObject.of("choices", List.of(JSONObject.of("finish_reason", "length", "message", JSONObject.of("content", "partial summary"))));
    try { KnowledgeModelService.completionText(result); fail("must reject truncation"); }
    catch (IllegalStateException expected) { assertTrue(expected.getMessage().contains("截断")); }
  }
  @Test public void completeNonEmptyModelOutputIsAccepted() {
    JSONObject result = JSONObject.of("choices", List.of(JSONObject.of("finish_reason", "stop", "message", JSONObject.of("content", " 摘要 "))));
    assertEquals("摘要", KnowledgeModelService.completionText(result));
  }
}
