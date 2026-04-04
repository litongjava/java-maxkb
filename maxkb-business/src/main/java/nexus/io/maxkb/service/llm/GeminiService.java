package nexus.io.maxkb.service.llm;

import lombok.extern.slf4j.Slf4j;
import nexus.io.gemini.GeminiClient;
import nexus.io.gemini.GoogleModels;
import nexus.io.openai.chat.OpenAiChatRequest;
import nexus.io.openai.client.OpenAiClient;
import nexus.io.openai.consts.OpenAiConst;
import nexus.io.tio.utils.environment.EnvUtils;
import okhttp3.Call;
import okhttp3.Callback;

@Slf4j
public class GeminiService {
  public String generate(String prompt) {
    String apiKey = EnvUtils.get("GEMINI_API_KEY");
    if (EnvUtils.isDev()) {
      log.info("api key:{}", apiKey);
    }
    return GeminiClient.chatWithModel(apiKey, GoogleModels.GEMINI_2_5_FLASH, "user", prompt);
    //return OpenAiClient.chatWithModel(OpenAiConstants.GEMINI_OPENAI_API_BASE, apiKey, GoogleGeminiModels.GEMINI_2_0_FLASH_EXP, "user", prompt);
  }
  public Call stream(OpenAiChatRequest chatRequestVo, Callback callback) {
    String apiKey = EnvUtils.get("GEMINI_API_KEY");
    Call call = OpenAiClient.chatCompletions(OpenAiConst.GEMINI_OPENAI_API_BASE, apiKey, chatRequestVo, callback);
    return call;
  }
}
