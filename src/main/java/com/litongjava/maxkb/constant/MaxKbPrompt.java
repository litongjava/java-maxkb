package com.litongjava.maxkb.constant;

public interface MaxKbPrompt {

  //@formatter:off
  String image_to_text = "You are a helpful ocr assiatant. Please extract the text from the following image and text.\r\n"
      + "- Ignore the header and footer, but exclude the title.\r\n"
      + "- Do not include any additional information or explanations\r\n"
      + "- Need to recognize mathematical, physical, and chemical symbols and formulas and keep format.\r\n"
      + "- Need to recognize table,title and page number.\r\n"
      + "- If you encounter an image, display a placeholder for the image.\r\n"
      + "- Output format is markdown.\r\n"
      + "- You can directly output Markdown without using Markdown tags.\r\n"
      + "- If you cannot recognize , please output \"not_working\"\r\n"
      + "markdown:";
  //@formatter:on
}