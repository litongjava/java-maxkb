package com.litongjava.maxkb.service.kb;

import org.postgresql.util.PGobject;

import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.kit.PgObjectUtils;
import com.litongjava.maxkb.model.MaxKbApplicationChat;
import com.litongjava.maxkb.model.MaxKbApplicationTempSetting;
import com.litongjava.maxkb.vo.MaxKbApplicationVo;
import com.litongjava.model.result.ResultVo;
import com.litongjava.tio.utils.snowflake.SnowflakeIdUtils;

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
