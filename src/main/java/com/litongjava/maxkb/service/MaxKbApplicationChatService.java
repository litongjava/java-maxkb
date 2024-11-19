package com.litongjava.maxkb.service;

import org.postgresql.util.PGobject;

import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Record;
import com.litongjava.kit.JsonFieldUtils;
import com.litongjava.maxkb.model.MaxKbApplicationChat;
import com.litongjava.maxkb.model.MaxKbApplicationTempSetting;
import com.litongjava.maxkb.vo.MaxKbApplicationVo;
import com.litongjava.model.result.ResultVo;
import com.litongjava.tio.utils.snowflake.SnowflakeIdUtils;

public class MaxKbApplicationChatService {

  public ResultVo open(String bodyString, MaxKbApplicationVo vo) {
    Long id = vo.getId();
    PGobject jsonb = JsonFieldUtils.jsonb(bodyString);
    Record record = Record.by("id", id).set("setting", jsonb);
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
