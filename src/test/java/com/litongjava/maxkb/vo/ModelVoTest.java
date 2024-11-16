package com.litongjava.maxkb.vo;

import org.junit.Test;

import com.litongjava.tio.utils.json.FastJson2Utils;
import com.litongjava.tio.utils.json.JsonUtils;

public class ModelVoTest {

  @Test
  public void testParse() {
    // @formatter:off
    String jsonString="{\r\n"
        + "  \"name\": \"text-embedding-3-large\",\r\n"
        + "  \"model_type\": \"EMBEDDING\",\r\n"
        + "  \"model_name\": \"text-embedding-3-large\",\r\n"
        + "  \"permission_type\": \"PRIVATE\",\r\n"
        + "  \"credential\":\r\n"
        + "  {\r\n"
        + "    \"api_base\": \"https://api.openai.com/v1\",\r\n"
        + "    \"api_key\": \"11111111\"\r\n"
        + "  },\r\n"
        + "  \"provider\": \"model_openai_provider\"\r\n"
        + "}";
    // @formatter:on

    ModelVo modelVo = FastJson2Utils.parse(jsonString, ModelVo.class);
    System.out.println(JsonUtils.toJson(modelVo));

  }

}
