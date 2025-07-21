package com.litongjava.maxkb.service.api;

import com.litongjava.gemini.GeminiClient;
import com.litongjava.gemini.GoogleModels;
import com.litongjava.openai.chat.OpenAiChatRequestVo;
import com.litongjava.openai.client.OpenAiClient;
import com.litongjava.openai.consts.OpenAiConstants;
import com.litongjava.tio.utils.environment.EnvUtils;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;

@Slf4j
public class GeminiService {
  public String generate(String prompt) {
    String apiKey = EnvUtils.get("GEMINI_API_KEY");
    if (EnvUtils.isDev()) {
      log.info("api key:{}", apiKey);
    }
    return GeminiClient.chatWithModel(apiKey, GoogleModels.GEMINI_2_0_FLASH_EXP, "user", prompt);
    //return OpenAiClient.chatWithModel(OpenAiConstants.GEMINI_OPENAI_API_BASE, apiKey, GoogleGeminiModels.GEMINI_2_0_FLASH_EXP, "user", prompt);
  }
  public Call stream(OpenAiChatRequestVo chatRequestVo, Callback callback) {
    String apiKey = EnvUtils.get("GEMINI_API_KEY");
    Call call = OpenAiClient.chatCompletions(OpenAiConstants.GEMINI_OPENAI_API_BASE, apiKey, chatRequestVo, callback);
    return call;
  }
}
