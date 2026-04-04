package nexus.io.maxkb.service.kb;

import org.postgresql.util.PGobject;

import nexus.io.db.activerecord.Db;
import nexus.io.db.activerecord.Row;
import nexus.io.kit.PgObjectUtils;
import nexus.io.maxkb.model.MaxKbApplicationChat;
import nexus.io.maxkb.model.MaxKbApplicationTempSetting;
import nexus.io.maxkb.vo.MaxKbApplicationVo;
import nexus.io.model.result.ResultVo;
import nexus.io.tio.utils.snowflake.SnowflakeIdUtils;

public class MaxKbApplicationChatService {

  public ResultVo open(String bodyString, MaxKbApplicationVo vo) {
    Long id = vo.getId();
    PGobject jsonb = PgObjectUtils.jsonb(bodyString);
    Row record = Row.by("id", id).set("setting", jsonb);
    if (Db.exists(MaxKbApplicationTempSetting.tableName, "id", id)) {
      Db.update(MaxKbApplicationTempSetting.tableName, record);
    } else {
      Db.save(MaxKbApplicationTempSetting.tableName, record);
    }

    long chatId = SnowflakeIdUtils.id();
    new MaxKbApplicationChat().setId(chatId).setApplicationId(id).setChatType(1).save();
    return ResultVo.ok(chatId);
  }

}
