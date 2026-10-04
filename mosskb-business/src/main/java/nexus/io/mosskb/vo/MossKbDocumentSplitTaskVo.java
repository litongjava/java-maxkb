package nexus.io.mosskb.vo;

import java.util.List;

import com.jfinal.kit.Kv;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 文档分段任务状态。
 *
 * <p>status 为 running 时 result 为空，前端继续轮询；success 时 result 是可直接提交的分段列表；
 * failed 时从 error_message 读取失败原因。
 */
@Data
@Accessors(chain = true)
public class MossKbDocumentSplitTaskVo {
  private Long task_id;
  private String status;
  private Integer progress;
  private Integer total;
  private String file_name;
  private Long file_size;
  private String errorMessage;
  private List<Kv> result;
}
