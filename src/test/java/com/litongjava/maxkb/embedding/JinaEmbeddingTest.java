package com.litongjava.maxkb.embedding;

import org.junit.Test;

import com.litongjava.openai.client.OpenAiClient;
import com.litongjava.openai.embedding.EmbeddingRequestVo;
import com.litongjava.openai.embedding.EmbeddingResponseVo;
import com.litongjava.tio.utils.json.JsonUtils;

public class JinaEmbeddingTest {

  @Test
  public void testEmbedding() {
    String input = "你好世界";
    String url = "http://192.168.3.9:10002/v1";
    // 因为调用的是本地模型可以随便写
    String apiKey = "1234";

    EmbeddingRequestVo embeddingRequestVo = new EmbeddingRequestVo(input);
    EmbeddingResponseVo embeddings = OpenAiClient.embeddings(url, apiKey, embeddingRequestVo);
    System.out.println(JsonUtils.toSkipNullJson(embeddings));
  }
}
