package com.litongjava.maxkb.service.api;

import com.litongjava.gemini.GoogleGeminiModels;
import com.litongjava.openai.chat.OpenAiChatRequestVo;
import com.litongjava.openai.chat.OpenAiChatResponseVo;
import com.litongjava.openai.client.OpenAiClient;
import com.litongjava.openai.constants.OpenAiConstants;
import com.litongjava.tio.utils.environment.EnvUtils;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;

@Slf4j
public class GeminiService {

  public OpenAiChatResponseVo generate(String prompt) {
    String apiKey = EnvUtils.get("GEMINI_API_KEY");
    log.info("api key:{}", apiKey);
    return OpenAiClient.chatWithModel(OpenAiConstants.GEMINI_OPENAI_API_BASE, apiKey, GoogleGeminiModels.GEMINI_2_0_FLASH_EXP, "user", prompt);
  }

  public Call stream(OpenAiChatRequestVo chatRequestVo, Callback callback) {
    String apiKey = EnvUtils.get("GEMINI_API_KEY");
    Call call = OpenAiClient.chatCompletions(OpenAiConstants.GEMINI_OPENAI_API_BASE, apiKey, chatRequestVo, callback);
    return call;
  }
}
