package com.litongjava.maxkb.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaxKbChatRecordDetail {
  private MaxKbChatStep chat_step;
  private MaxKbRetrieveResult search_step;
}
