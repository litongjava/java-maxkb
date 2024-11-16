package com.litongjava.maxkb;

import java.io.IOException;

import com.litongjava.openai.client.OpenAiClient;
import com.litongjava.openai.embedding.EmbeddingRequestVo;
import com.litongjava.openai.embedding.EmbeddingResponseVo;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.json.JsonUtils;

import okhttp3.Response;

public class EmbeddingExample {

  public static void main(String[] args) {
    // load config
    EnvUtils.load();
    // request model
    EmbeddingRequestVo embeddingRequestVo = new EmbeddingRequestVo();
    embeddingRequestVo.input("What's your name").model("text-embedding-3-small");

    String bodyString = JsonUtils.toJson(embeddingRequestVo);
    // send request
    try (Response response = OpenAiClient.embeddings(bodyString)) {
      if (response.isSuccessful()) {
        // get response string
        String string = response.body().string();
        System.out.println(string);
        // process the resposne data
        EmbeddingResponseVo responseVo = JsonUtils.parse(string, EmbeddingResponseVo.class);
        System.out.println(JsonUtils.toJson(responseVo));
      } else {
        System.out.println(response);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}