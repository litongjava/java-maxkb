package com.litongjava.maxkb.service.kb;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.postgresql.util.PGobject;

import com.jfinal.kit.Kv;
import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.maxkb.constant.MaxKbTableNames;
import com.litongjava.maxkb.vo.MaxKbChatRecordDetail;
import com.litongjava.maxkb.vo.ParagraphSearchResultVo;
import com.litongjava.model.result.ResultVo;
import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.utils.json.JsonUtils;

public class MaxKbApplicationCharRecordService {

  public ResultVo get(Long userId, Long applicationId, Long chatId, Long recordId) {
    Row queryRecord = Row.by("id", recordId).set("chat_id", chatId);
    Row record = Db.findFirst(MaxKbTableNames.max_kb_application_chat_record, queryRecord);
    if (record != null) {
      Object object = record.get("details");
      record.remove("details");
      MaxKbChatRecordDetail detail = null;
      if (object instanceof PGobject) {
        PGobject pgObject1 = (PGobject) object;
        String value = pgObject1.getValue();
        if (StrUtil.isNotBlank(value)) {
          detail = JsonUtils.parse(value, MaxKbChatRecordDetail.class);
        }
      } else if (object instanceof String) {
        String value = (String) object;
        if (StrUtil.isNotBlank(value)) {
          detail = JsonUtils.parse(value, MaxKbChatRecordDetail.class);
        }
      }

      Kv kv = record.toKv();
      if (detail != null) {
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
