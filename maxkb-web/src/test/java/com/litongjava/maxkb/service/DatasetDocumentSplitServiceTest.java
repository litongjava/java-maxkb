package com.litongjava.maxkb.service;

import java.net.URL;

import org.junit.Test;

import com.litongjava.openai.chat.OpenAiChatResponse;
import com.litongjava.openai.client.OpenAiClient;
import com.litongjava.tio.utils.hutool.FileUtil;
import com.litongjava.tio.utils.hutool.FilenameUtils;
import com.litongjava.tio.utils.hutool.ResourceUtil;
import com.litongjava.tio.utils.json.JsonUtils;

public class DatasetDocumentSplitServiceTest {

  @Test
  public void imageToMarkDown() {
    String apiKey = "";
    String prompt = "Convert the image to text and just output the text.\r\ntext";

    String filePath = "images/200-dpi.png";
    URL url = ResourceUtil.getResource(filePath);
    byte[] readUrlAsBytes = FileUtil.readBytes(url);
    String suffix = FilenameUtils.getSuffix(filePath);
    OpenAiChatResponse chatWithImage = OpenAiClient.chatWithImage(apiKey, prompt, readUrlAsBytes, suffix);
    System.out.println(JsonUtils.toJson(chatWithImage));
    String content = chatWithImage.getChoices().get(0).getMessage().getContent();
    System.out.println(content);
  }
}
