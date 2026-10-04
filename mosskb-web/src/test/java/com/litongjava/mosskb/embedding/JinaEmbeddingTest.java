package com.litongjava.mosskb.embedding;

import org.junit.Assume;
import org.junit.Test;

import nexus.io.openai.client.OpenAiClient;
import nexus.io.openai.embedding.EmbeddingRequest;
import nexus.io.openai.embedding.EmbeddingResponse;
import nexus.io.tio.utils.json.JsonUtils;

public class JinaEmbeddingTest {

  @Test
  public void testEmbedding() {
    // 这个用例依赖局域网里的本地向量服务，默认跳过；设置 JINA_EMBEDDING_URL 后再跑。
    String url = System.getenv("JINA_EMBEDDING_URL");
    Assume.assumeTrue("未配置 JINA_EMBEDDING_URL，跳过本地向量服务联调", url != null && !url.isBlank());

    String input = "你好世界";
    // 因为调用的是本地模型可以随便写
    String apiKey = "1234";

    EmbeddingRequest embeddingRequestVo = new EmbeddingRequest(input);
    EmbeddingResponse embeddings = OpenAiClient.embeddings(url, apiKey, embeddingRequestVo);
    System.out.println(JsonUtils.toSkipNullJson(embeddings));
  }
}
