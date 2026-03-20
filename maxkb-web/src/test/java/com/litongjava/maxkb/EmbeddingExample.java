package com.litongjava.maxkb;

import java.io.IOException;

import com.litongjava.openai.client.OpenAiClient;
import com.litongjava.openai.embedding.EmbeddingRequest;
import com.litongjava.openai.embedding.EmbeddingResponse;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.json.JsonUtils;

public class EmbeddingExample {

  public static void main(String[] args) {
    // load config
    EnvUtils.load();
    // request model
    EmbeddingRequest embeddingRequestVo = new EmbeddingRequest();
    embeddingRequestVo.input("What's your name").model("text-embedding-3-small");

    String bodyString = JsonUtils.toJson(embeddingRequestVo);
    // send request
    try (okhttp3.Response response = OpenAiClient.embeddings(bodyString)) {
      if (response.isSuccessful()) {
        // get response string
        String string = response.body().string();
        System.out.println(string);
        // process the resposne data
        EmbeddingResponse responseVo = JsonUtils.parse(string, EmbeddingResponse.class);
        System.out.println(JsonUtils.toJson(responseVo));
      } else {
        System.out.println(response);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}