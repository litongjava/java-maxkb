package nexus.io.mosskb.vo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProbrolemCreateBatch {
  private List<Long> problem_id_list;
  private List<MossKbParagraphId> paragraph_list;
}
