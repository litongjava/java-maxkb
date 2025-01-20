package com.litongjava.maxkb.service.api;

import java.util.Collections;

import com.litongjava.gemini.GeminiChatRequestVo;
import com.litongjava.gemini.GeminiChatResponseVo;
import com.litongjava.gemini.GeminiClient;
import com.litongjava.gemini.GeminiContentVo;
import com.litongjava.gemini.GeminiPartVo;
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
    // 1. 构造请求体
    GeminiPartVo part = new GeminiPartVo(prompt);
    GeminiContentVo content = new GeminiContentVo("user", Collections.singletonList(part));
    GeminiChatRequestVo reqVo = new GeminiChatRequestVo(Collections.singletonList(content));
    
    GeminiChatResponseVo generate = GeminiClient.generate(apiKey, GoogleGeminiModels.GEMINI_2_0_FLASH_EXP, reqVo);
    
    return null;
    //return OpenAiClient.chatWithModel(OpenAiConstants.GEMINI_OPENAI_API_BASE, apiKey, GoogleGeminiModels.GEMINI_2_0_FLASH_EXP, "user", prompt);
  }

  public Call stream(OpenAiChatRequestVo chatRequestVo, Callback callback) {
    String apiKey = EnvUtils.get("GEMINI_API_KEY");
    Call call = OpenAiClient.chatCompletions(OpenAiConstants.GEMINI_OPENAI_API_BASE, apiKey, chatRequestVo, callback);
    return call;
  }
}
