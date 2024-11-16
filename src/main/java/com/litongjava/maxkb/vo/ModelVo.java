package com.litongjava.maxkb.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
{
  "name": "text-embedding-3-large",
  "model_type": "EMBEDDING",
  "model_name": "text-embedding-3-large",
  "permission_type": "PRIVATE",
  "credential":
  {
    "api_base": "https://api.openai.com/v1",
    "api_key": "11111111"
  },
  "provider": "model_openai_provider"
}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain=true)
public class ModelVo {
  private Long id;
  private String name, model_type, model_name, permission_type, provider;
  private CredentialVo credential;
}
