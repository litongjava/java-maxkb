package nexus.io.mosskb.vo;

import com.alibaba.fastjson2.JSONObject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain=true)
public class KbDatasetModel {
  private Long id;
  private String name,desc;
  private Long embedding_mode_id;
  /** 知识库类型：0 通用、1 Web 站点、2 飞书文档，与 moss_kb_dataset.type 的字符串取值一致。 */
  private String type;
  /** Web 站点等类型知识库的扩展信息，例如 source_url、selector。 */
  private JSONObject meta;

}
