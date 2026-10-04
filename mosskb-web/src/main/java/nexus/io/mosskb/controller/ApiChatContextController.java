package nexus.io.mosskb.controller;

import nexus.io.annotation.Get;
import nexus.io.annotation.Post;
import nexus.io.annotation.RequestPath;
import nexus.io.db.activerecord.Db;
import nexus.io.db.activerecord.Row;
import nexus.io.jfinal.aop.Aop;
import nexus.io.mosskb.service.kb.ApplicationAccess;
import nexus.io.mosskb.service.kb.ChatExecution;
import nexus.io.mosskb.service.kb.ConversationContextService;
import nexus.io.model.result.ResultVo;
import nexus.io.tio.boot.http.TioRequestContext;
import com.alibaba.fastjson2.JSONObject;

@RequestPath("/api/application")
public class ApiChatContextController {
  private static final String FIND_CONTEXT = """
      select summary,through_record_id,compacted_rounds,revision from moss_kb_chat_context where chat_id=?
      """;

  @Get("/{app}/chat/{chat}/context")
  public ResultVo get(Long app, Long chat) {
    if (!ApplicationAccess.canReadChat(TioRequestContext.getUserIdLong(), app, chat)) {
      return ResultVo.fail("会话不存在或无权访问");
    }
    Row row = Db.findFirst(FIND_CONTEXT, chat);
    return ResultVo.ok(row == null ? JSONObject.of("summary", "", "revision", 0, "compacted_rounds", 0) : row.toKv());
  }

  @Post("/{app}/chat/{chat}/context/compact")
  public ResultVo compact(Long app, Long chat) {
    if (!ApplicationAccess.canReadChat(TioRequestContext.getUserIdLong(), app, chat)) {
      return ResultVo.fail("会话不存在或无权访问");
    }
    if (!ChatExecution.begin(chat)) {
      return ResultVo.fail("会话正在生成回答，请完成后再压缩");
    }
    try {
      Integer keep = Db.queryInt("select dialogue_number from moss_kb_application where id=?", app);
      return ResultVo.ok(Aop.get(ConversationContextService.class).load(chat, Long.MAX_VALUE, keep, true).metadata());
    } finally {
      ChatExecution.end(chat);
    }
  }
}
