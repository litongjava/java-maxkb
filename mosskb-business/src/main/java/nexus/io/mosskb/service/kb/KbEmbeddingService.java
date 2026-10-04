package nexus.io.mosskb.service.kb;

import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import org.postgresql.util.PGobject;
import com.google.common.util.concurrent.Striped;
import nexus.io.chat.PlatformInput;
import nexus.io.db.activerecord.Db;
import nexus.io.db.activerecord.Row;
import nexus.io.db.utils.PgVectorUtils;
import nexus.io.tio.utils.crypto.Md5Utils;
import nexus.io.tio.utils.snowflake.SnowflakeIdUtils;

/** Cache keys isolate model instances; documents and queries use the same model. */
public class KbEmbeddingService {
  private static final Striped<Lock> LOCKS = Striped.lock(256);
  public Long getVectorId(String text) { return cached(text).getLong("id"); }
  public Long getVectorId(String text, String model) { return getVectorId(text); }
  public Long getVectorId(String text, int areaCode, String model) { return getVectorId(text); }
  public Long getVectorId(String text, PlatformInput input) { return getVectorId(text); }
  public PGobject getVector(String text) { return cached(text).get("v"); }
  public PGobject getVector(String text, String model) { return getVector(text); }
  public PGobject getVector(String text, PlatformInput input) { return getVector(text); }
  public float[] embedding(String text, PlatformInput input) { return KnowledgeModelService.embedding(text); }
  public PGobject getVectorForModel(String text, Long modelId) { return cached(text, modelId).get("v"); }
  public Long getVectorIdForModel(String text, Long modelId) { return cached(text, modelId).getLong("id"); }
  private Row cached(String text) { return cached(text, 1002L); }
  private Row cached(String text, Long modelId) {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("Embedding text must not be empty");
    }
    String hash = Md5Utils.md5Hex(text);
    long resolvedId = modelId == null ? 1002L : modelId;
    Row selected = Db.findById("moss_kb_model", resolvedId);
    if (selected == null || !"EMBEDDING".equals(selected.getStr("model_type"))) {
      throw new IllegalArgumentException("知识库配置的向量模型不存在");
    }
    nexus.io.mosskb.vo.CredentialVo credential = ModelCatalogService.credential(selected);
    String base = ModelCatalogService.baseUrl(credential.getApi_base());
    String model = resolvedId == 1002L ? KnowledgeModelService.embeddingModel() + ":1024"
        : resolvedId + ":" + Md5Utils.md5Hex(base + "|" + selected.getStr("model_name")) + ":1024";
    Lock lock = LOCKS.get(model + hash); lock.lock();
    try {
      Row row = Db.findFirst("select id,v from moss_kb_embedding_cache where md5=? and m=?", hash, model);
      if (row != null) {
        return row;
      }
      com.alibaba.fastjson2.JSONObject body = new com.alibaba.fastjson2.JSONObject();
      body.put("model", selected.getStr("model_name")); body.put("input", java.util.List.of(text)); body.put("dimensions", 1024);
      float[] vector;
      try (okhttp3.Response response = nexus.io.openai.client.OpenAiClient.embeddings(base, credential.getApi_key(), body.toJSONString())) {
        if (!response.isSuccessful() || response.body() == null) {
          throw new IllegalStateException("向量请求失败，HTTP " + response.code());
        }
        vector = com.alibaba.fastjson2.JSON.parseObject(response.body().string()).getJSONArray("data").getJSONObject(0).getObject("embedding", float[].class);
      } catch (java.io.IOException e) {
        throw new IllegalStateException("向量服务连接失败", e);
      }
      if (vector == null || vector.length != 1024) {
        throw new IllegalStateException("向量模型必须返回 1024 维");
      }
      for (float value : vector) {
        if (!Float.isFinite(value)) {
          throw new IllegalStateException("向量模型返回非有限数值");
        }
      }
      row = Row.by("id", SnowflakeIdUtils.id()).set("t", text).set("md5", hash).set("m", model)
          .set("v", PgVectorUtils.getPgVector(Arrays.toString(vector)));
      Db.save("moss_kb_embedding_cache", row); return row;
    } finally { lock.unlock(); }
  }
}
