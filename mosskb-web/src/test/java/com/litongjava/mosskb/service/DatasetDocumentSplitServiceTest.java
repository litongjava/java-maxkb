package com.litongjava.mosskb.service;

import java.net.URL;

import org.junit.Assume;
import org.junit.Test;

import nexus.io.openai.chat.OpenAiChatResponse;
import nexus.io.openai.client.OpenAiClient;
import nexus.io.tio.utils.hutool.FileUtil;
import nexus.io.tio.utils.hutool.FilenameUtils;
import nexus.io.tio.utils.hutool.ResourceUtil;
import nexus.io.tio.utils.json.JsonUtils;

public class DatasetDocumentSplitServiceTest {

  @Test
  public void imageToMarkDown() {
    String filePath = "images/200-dpi.png";
    URL url = ResourceUtil.getResource(filePath);
    Assume.assumeTrue("测试图片不在 classpath 上：" + filePath + "，跳过", url != null);

    // 该用例要真的调用远程多模态接口，默认跳过；配置 OPENAI_API_KEY 后再跑。
    String apiKey = System.getenv("OPENAI_API_KEY");
    Assume.assumeTrue("未配置 OPENAI_API_KEY，跳过图像转文本联调", apiKey != null && !apiKey.isBlank());

    String prompt = "Convert the image to text and just output the text.\r\ntext";

    byte[] readUrlAsBytes = FileUtil.readBytes(url);
    String suffix = FilenameUtils.getSuffix(filePath);
    OpenAiChatResponse chatWithImage = OpenAiClient.chatWithImage(apiKey, prompt, readUrlAsBytes, suffix);
    System.out.println(JsonUtils.toJson(chatWithImage));
    String content = chatWithImage.getChoices().get(0).getMessage().getContent();
    System.out.println(content);
  }
}
