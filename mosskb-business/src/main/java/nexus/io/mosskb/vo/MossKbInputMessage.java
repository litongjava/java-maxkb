package nexus.io.mosskb.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MossKbInputMessage {
  private String role;
  private String content;
}
