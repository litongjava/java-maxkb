package com.litongjava.maxkb.constant;

public interface MaxKbPrompt {

  //@formatter:off
  String image_to_text = "Please extract the pure text from the following image and text\r\n"
      + "- Ignore header and footer\r\n"
      + "- Do not include any additional information or explanations\r\n"
      + "- Need to recognize mathematical, physical, and chemical symbols and formulas.\r\n"
      + "- Output only the text portion.\r\n"
      + "- If you cannot recognize , please output \"not_working\"\r\n"
      + "text:";
  //@formatter:on
}