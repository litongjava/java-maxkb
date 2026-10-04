package nexus.io.mosskb.dao;

import nexus.io.db.activerecord.Db;
import nexus.io.db.activerecord.Row;
import nexus.io.mosskb.model.MossKbApplication;

public class MossKbApplicationDao {

  public Row getBasicInfoById(Long applicationId) {
    String columns = "id,name,desc,prologue,dialogue_number,icon,type,stt_model_id,tts_model_id,stt_model_enable,tts_model_enable,tts_type,work_flow,show_source,multiple_rounds_dialogue";
    Row row = Db.findColumnsById(MossKbApplication.tableName, columns, applicationId);
    if (row != null) {
      row.set("applicaiton_type", row.getObject("type"));
    }

    return row;
  }

}
