package com.litongjava.maxkb.vo;

import java.util.List;

import com.litongjava.chat.UniChatMessage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MaxKbChatStep {
  private Integer cost;
  private Long model_id;
  private Double run_time;
  private String step_type;
  private List<UniChatMessage> message_list;
  private Integer answer_tokens;
  private Integer message_tokens;
}
