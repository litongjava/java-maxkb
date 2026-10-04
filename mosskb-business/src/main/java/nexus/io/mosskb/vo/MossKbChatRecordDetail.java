package nexus.io.mosskb.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MossKbChatRecordDetail {
  private MossKbChatStep chat_step;
  private MossKbRetrieveResult search_step;
}
