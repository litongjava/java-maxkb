package com.litongjava.maxkb.client;

import java.util.Arrays;

import com.litongjava.openai.client.OpenAiClient;
import com.litongjava.openai.consts.OpenAiModels;
import com.litongjava.openai.embedding.EmbeddingRequestVo;
import com.litongjava.openai.embedding.EmbeddingResponseVo;

public class RumiClient {

  public String embedding(String content) {
    String serverUrl = "http://java-api.rumibot.com";
    String apiKey = "";

    EmbeddingRequestVo reqVo = new EmbeddingRequestVo(OpenAiModels.TEXT_EMBEDDING_3_LARGE, content);

    EmbeddingResponseVo embeddings = OpenAiClient.embeddings(serverUrl, apiKey, reqVo);

    float[] embeddingArray = embeddings.getData().get(0).getEmbedding();
    String string = Arrays.toString(embeddingArray);
    return string;
  }
}
