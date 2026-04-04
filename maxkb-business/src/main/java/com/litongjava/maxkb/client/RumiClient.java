package com.litongjava.maxkb.client;

import java.util.Arrays;

import nexus.io.openai.client.OpenAiClient;
import nexus.io.openai.consts.OpenAiModels;
import nexus.io.openai.embedding.EmbeddingRequest;
import nexus.io.openai.embedding.EmbeddingResponse;

public class RumiClient {

  public String embedding(String content) {
    String serverUrl = "http://java-api.rumibot.com";
    String apiKey = "";

    EmbeddingRequest reqVo = new EmbeddingRequest(OpenAiModels.TEXT_EMBEDDING_3_LARGE, content);

    EmbeddingResponse embeddings = OpenAiClient.embeddings(serverUrl, apiKey, reqVo);

    float[] embeddingArray = embeddings.getData().get(0).getEmbedding();
    String string = Arrays.toString(embeddingArray);
    return string;
  }
}
