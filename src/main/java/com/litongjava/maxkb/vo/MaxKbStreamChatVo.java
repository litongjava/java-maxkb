package com.litongjava.maxkb.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MaxKbStreamChatVo {
  private Long chat_id;
  private Long id;
  private Boolean operate;
  private String content;
  private Boolean is_end;
}
