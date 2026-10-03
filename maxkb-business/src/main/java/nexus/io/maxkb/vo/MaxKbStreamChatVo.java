package nexus.io.maxkb.vo;

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
  private String chat_record_id;
  private String node_id = "ai-chat-node";
  private String real_node_id = "ai-chat-node";
  private String runtime_node_id = "ai-chat-node";
  private String up_node_id = "start-node";
  private String node_type = "ai-chat-node";
  private String view_type = "many_view";
  private String reasoning_content = "";
  private Boolean node_is_end = false;
}
