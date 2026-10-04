package nexus.io.mosskb.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class MossKbUpdateDocumentRequestVo {
  private String name;
  private Boolean is_active;
  private String hit_handling_method;
  private Double directly_return_similarity;
}
