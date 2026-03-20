package com.litongjava.maxkb.client;

import java.util.Arrays;

import com.litongjava.openai.client.OpenAiClient;
import com.litongjava.openai.embedding.EmbeddingRequest;
import com.litongjava.openai.embedding.EmbeddingResponse;

public class RumiClient {

  public String embedding(String content) {
    String serverUrl = "http://java-api.rumibot.com";
    String apiKey = "";

    EmbeddingRequest reqVo = new EmbeddingRequest(com.litongjava.openai.consts.OpenAiModels.TEXT_EMBEDDING_3_LARGE, content);

    EmbeddingResponse embeddings = OpenAiClient.embeddings(serverUrl, apiKey, reqVo);

    float[] embeddingArray = embeddings.getData().get(0).getEmbedding();
    String string = Arrays.toString(embeddingArray);
    return string;
  }
}
