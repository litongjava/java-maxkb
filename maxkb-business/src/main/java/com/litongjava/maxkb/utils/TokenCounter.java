package com.litongjava.maxkb.utils;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;

public class TokenCounter {
  // 将 EncodingRegistry 和 Encoding 设为静态成员，以便复用
  private static final EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
  private static final Encoding encoding = registry.getEncodingForModel(ModelType.GPT_4); 

  public static int countTokens(String text) {
    // 直接使用已复用的 encoding 实例
    return encoding.countTokens(text);
  }
}
