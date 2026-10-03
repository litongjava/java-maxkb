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

  public ResultVo openPublished(Long applicationId) {
    Long clientId = nexus.io.tio.boot.http.TioRequestContext.getUserIdLong();
    if (!ApplicationAccess.canChat(clientId, applicationId)) {
      return ResultVo.fail("应用不存在或无权访问");
    }
    long chatId = SnowflakeIdUtils.id();
    new MaxKbApplicationChat().setId(chatId).setApplicationId(applicationId).setClientId(clientId).setChatType(0).save();
    return ResultVo.ok(chatId);
  }

  public ResultVo open(String bodyString, MaxKbApplicationVo vo) {
    Long id = vo.getId();
    Long userId = nexus.io.tio.boot.http.TioRequestContext.getUserIdLong();
    Row app = Db.findById("max_kb_application", id);
    if (userId == null || app == null || (userId != 1L && !userId.equals(app.getLong("user_id")))) {
      return ResultVo.fail("应用不存在或无权访问");
    }
    long chatId = SnowflakeIdUtils.id();
    PGobject jsonb = PgObjectUtils.jsonb(bodyString);
    Db.tx(() -> {
      Db.save(MaxKbApplicationTempSetting.tableName, Row.by("id", chatId).set("setting", jsonb));
      new MaxKbApplicationChat().setId(chatId).setApplicationId(id).setClientId(userId).setChatType(1).save();
      return true;
    });
    return ResultVo.ok(chatId);
  }

}
