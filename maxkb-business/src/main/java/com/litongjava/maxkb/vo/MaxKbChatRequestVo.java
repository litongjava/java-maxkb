package com.litongjava.maxkb.vo;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MaxKbChatRequestVo {
  private String message;
  private Boolean re_chat;
  private Map<String, Object> form_data;
}
