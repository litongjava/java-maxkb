package nexus.io.maxkb.controller;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import nexus.io.annotation.Delete;
import nexus.io.annotation.Get;
import nexus.io.annotation.Post;
import nexus.io.annotation.Put;
import nexus.io.annotation.RequestPath;
import nexus.io.jfinal.aop.Aop;
import nexus.io.maxkb.service.kb.MaxKbChatHistoryService;
import nexus.io.maxkb.service.kb.MaxKbChatLogExportService;
import nexus.io.maxkb.service.kb.MaxKbChatLogService;
import nexus.io.model.result.ResultVo;
import nexus.io.tio.boot.http.TioRequestContext;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.http.server.util.Resps;

@RequestPath("/api/application")
public class ApiChatHistoryController {
  private final MaxKbChatHistoryService service = Aop.get(MaxKbChatHistoryService.class);

  @Get("/{app}/chat/client/{page}/{size}")
  public ResultVo clientChats(Long app, Integer page, Integer size) {
    return service.chats(TioRequestContext.getUserIdLong(), app, page, size, true);
  }

  @Get("/{app}/chat/{page}/{size}")
  public ResultVo adminChats(Long app, Integer page, Integer size, HttpRequest request) {
    return service.chats(TioRequestContext.getUserIdLong(), app, page, size, false, query(request));
  }

  @Get("/{app}/chat/{chat}/chat_record/{page}/{size}")
  public ResultVo records(Long app, Long chat, Integer page, Integer size, HttpRequest request) {
    return service.records(TioRequestContext.getUserIdLong(), app, chat, page, size, !"false".equals(request.getParam("order_asc")));
  }

  @Put("/{app}/chat/client/{chat}")
  public ResultVo rename(Long app, Long chat, HttpRequest request) {
    return service.rename(TioRequestContext.getUserIdLong(), app, chat, JSON.parseObject(request.getBodyString()).getString("abstract"));
  }

  @Delete("/{app}/chat/client/{chat}")
  public ResultVo removeClient(Long app, Long chat) {
    return service.remove(TioRequestContext.getUserIdLong(), app, chat);
  }

  @Delete("/{app}/chat/{chat}")
  public ResultVo remove(Long app, Long chat) {
    return service.remove(TioRequestContext.getUserIdLong(), app, chat);
  }

  @Put("/{app}/chat/{chat}/chat_record/{record}/vote")
  public ResultVo vote(Long app, Long chat, Long record, HttpRequest request) {
    return service.vote(TioRequestContext.getUserIdLong(), app, chat, record, JSON.parseObject(request.getBodyString()).getString("vote_status"));
  }

  /** 导出勾选的会话；未勾选时按当前筛选条件导出。 */
  @Post("/{app}/chat/export")
  public HttpResponse export(Long app, HttpRequest request) {
    Long userId = TioRequestContext.getUserIdLong();
    MaxKbChatHistoryService.Query query = query(request);
    JSONObject body = body(request);
    if (body != null && body.getJSONArray("select_ids") != null) {
      List<Long> selectIds = new ArrayList<>();
      for (Object value : body.getJSONArray("select_ids")) {
        if (value != null) {
          selectIds.add(Long.valueOf(value.toString()));
        }
      }
      query.selectIds = selectIds;
    }
    MaxKbChatLogExportService exportService = Aop.get(MaxKbChatLogExportService.class);
    byte[] data = exportService.export(userId, app, query);
    return Resps.excel(TioRequestContext.getResponse(), data, exportService.fileName(app));
  }

  /** 标注：把答案作为段落写入所选文档，并记录在问答上。 */
  @Put("/{app}/chat/{chat}/chat_record/{record}/dataset/{dataset}/document_id/{document}/improve")
  public ResultVo improve(Long app, Long chat, Long record, Long dataset, Long document, HttpRequest request) {
    return Aop.get(MaxKbChatLogService.class).improve(TioRequestContext.getUserIdLong(), app, chat, record, dataset, document, body(request));
  }

  /** 当前记录的标注段落列表。 */
  @Get("/{app}/chat/{chat}/chat_record/{record}/improve")
  public ResultVo listImprove(Long app, Long chat, Long record) {
    return Aop.get(MaxKbChatLogService.class).listImprove(TioRequestContext.getUserIdLong(), app, chat, record);
  }

  @Delete("/{app}/chat/{chat}/chat_record/{record}/dataset/{dataset}/document_id/{document}/improve/{paragraph}")
  public ResultVo deleteImprove(Long app, Long chat, Long record, Long dataset, Long document, Long paragraph) {
    return Aop.get(MaxKbChatLogService.class).deleteImprove(TioRequestContext.getUserIdLong(), app, chat, record, dataset, document, paragraph);
  }

  /** 加入知识库：把勾选会话的问答批量写成段落。 */
  @Post("/{app}/dataset/{dataset}/improve")
  public ResultVo improveBatch(Long app, Long dataset, HttpRequest request) {
    return Aop.get(MaxKbChatLogService.class).improveBatch(TioRequestContext.getUserIdLong(), app, dataset, body(request));
  }

  private MaxKbChatHistoryService.Query query(HttpRequest request) {
    MaxKbChatHistoryService.Query query = new MaxKbChatHistoryService.Query();
    query.abstractText = request.getParam("abstract");
    query.startTime = request.getParam("start_time");
    query.endTime = request.getParam("end_time");
    query.minStar = request.getInt("min_star");
    query.minTrample = request.getInt("min_trample");
    query.comparer = request.getParam("comparer");
    return query;
  }

  private JSONObject body(HttpRequest request) {
    String bodyString = request.getBodyString();
    return bodyString == null || bodyString.isBlank() ? new JSONObject() : JSON.parseObject(bodyString);
  }
}
