package nexus.io.mosskb.service.kb;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.postgresql.util.PGobject;

import com.jfinal.kit.Kv;

import nexus.io.db.activerecord.Db;
import nexus.io.db.activerecord.Row;
import nexus.io.mosskb.constant.MossKbTableNames;
import nexus.io.mosskb.vo.MossKbChatRecordDetail;
import nexus.io.mosskb.vo.ParagraphSearchResultVo;
import nexus.io.model.result.ResultVo;
import nexus.io.tio.utils.hutool.StrUtil;
import nexus.io.tio.utils.json.JsonUtils;

public class MossKbApplicationCharRecordService {

  public ResultVo get(Long userId, Long applicationId, Long chatId, Long recordId) {
    if (!ApplicationAccess.canReadChat(userId, applicationId, chatId)) {
      return ResultVo.fail("会话不存在或无权访问");
    }
    Row queryRecord = Row.by("id", recordId).set("chat_id", chatId);
    Row record = Db.findFirst(MossKbTableNames.moss_kb_application_chat_record, queryRecord);
    if (record != null) {
      Object object = record.get("details");
      record.remove("details");
      // BIGINT[] 经 JDBC 返回为 java.sql.Array，统一成数组交给界面
      Object improveParagraphIds = record.get("improve_paragraph_id_list");
      record.remove("improve_paragraph_id_list");
      MossKbChatRecordDetail detail = null;
      if (object instanceof PGobject) {
        PGobject pgObject1 = (PGobject) object;
        String value = pgObject1.getValue();
        if (StrUtil.isNotBlank(value)) {
          detail = JsonUtils.parse(value, MossKbChatRecordDetail.class);
        }
      } else if (object instanceof String) {
        String value = (String) object;
        if (StrUtil.isNotBlank(value)) {
          detail = JsonUtils.parse(value, MossKbChatRecordDetail.class);
        }
      }

      Kv kv = record.toKv();
      String answer = record.getStr("answer_text");
      kv.set("answer_text_list", java.util.List.of(answer == null ? "" : answer));
      kv.set("improve_paragraph_id_list", ChatLogArrays.toLongList(improveParagraphIds));
      kv.set("create_time", java.util.Objects.toString(record.getObject("create_time")));
      kv.set("update_time", java.util.Objects.toString(record.getObject("update_time")));
      if (detail != null) {
        kv.set("agent_trace", detail.getSearch_step().getIterations());
        kv.set("agent_stop_reason", detail.getSearch_step().getStop_reason());
        kv.set("context_info", detail.getSearch_step().getContext());
        List<ParagraphSearchResultVo> paragraph_list = detail.getSearch_step().getParagraph_list();
        List<Kv> dataset_list = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();
        for (ParagraphSearchResultVo paragraphSearchResultVo : paragraph_list) {
          Long dataset_id = paragraphSearchResultVo.getDataset_id();
          String dataset_name = paragraphSearchResultVo.getDataset_name();

          if (seenIds.add(dataset_id)) {
            dataset_list.add(Kv.by("id", dataset_id).set("name", dataset_name));
          }
        }
        kv.set("paragraph_list", paragraph_list);
        kv.set("dataset_list", dataset_list);
      }
      return ResultVo.ok(kv);

    }

    return ResultVo.ok();
  }

}
