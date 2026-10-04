package nexus.io.mosskb.service.kb;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.postgresql.util.PGobject;

import nexus.io.db.activerecord.Db;
import nexus.io.db.activerecord.Row;
import nexus.io.mosskb.vo.MossKbChatRecordDetail;
import nexus.io.mosskb.vo.ParagraphSearchResultVo;
import nexus.io.table.utils.EasyExcelUtils;
import nexus.io.tio.utils.hutool.StrUtil;
import nexus.io.tio.utils.json.JsonUtils;

/**
 * 对话日志导出，使用 EasyExcel 写入 xlsx。
 *
 * 每个会话按问答逐行展开，列与官方界面一致：会话、问题、改写后的问题、回答、评价、
 * 引用分段、标注段落、用户、token、耗时与提问时间。检索与标注信息取自记录详情，
 * 与界面看到的内容同源。
 *
 * 表头用显式 head 声明，而不是让 EasyExcel 从 Row 的列名推导：Row 的列是哈希序，
 * 那样导出的列顺序是随机的。
 */
public class MossKbChatLogExportService {

  private static final String[] HEADERS = { "会话 ID", "标题", "用户问题", "改写后的问题", "回答", "用户反馈", "引用分段数", "分段标题 + 内容", "标注", "用户", "消耗 tokens", "耗时(秒)", "提问时间" };
  private static final Map<String, String> VOTE_LABELS = Map.of("-1", "未投票", "0", "赞同", "1", "反对");

  /** 导出文件名带日期，避免多次下载互相覆盖。 */
  public String fileName(Long applicationId) {
    Row application = Db.findById("moss_kb_application", applicationId);
    String name = application == null ? null : application.getStr("name");
    if (name == null || name.isBlank()) {
      name = "chat_log";
    }
    return name + "_" + LocalDate.now() + ".xlsx";
  }

  public byte[] export(Long user, Long applicationId, MossKbChatHistoryService.Query query) {
    MossKbChatHistoryService history = new MossKbChatHistoryService();
    List<Row> chats = history.chatsForExport(user, applicationId, query);
    List<List<Object>> rows = new ArrayList<>();
    for (Row chat : chats) {
      Long chatId = chat.getLong("id");
      String abstractText = chat.getStr("abstract");
      String asker = askerName(chat);
      List<Row> records = Db.find("select id,problem_text,answer_text,vote_status,message_tokens,answer_tokens,run_time,details,improve_paragraph_id_list,create_time"
          + " from moss_kb_application_chat_record where chat_id=? order by id", chatId);
      for (Row record : records) {
        rows.add(toRow(chatId, abstractText, asker, record));
      }
    }

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    // 与 api-table 的导出共用写入器：列宽自适应、日期与数组转换器
    EasyExcelUtils.getExcelWriteBuilder(outputStream)
        .sheet("对话日志")
        .head(EasyExcelUtils.head(HEADERS))
        .doWrite(rows);
    return outputStream.toByteArray();
  }

  private List<Object> toRow(Long chatId, String abstractText, String asker, Row record) {
    MossKbChatRecordDetail detail = parseDetail(record.get("details"));
    List<ParagraphSearchResultVo> paragraphList = detail == null ? List.of() : detail.getSearch_step().getParagraph_list();
    String rewritten = detail == null ? null : detail.getSearch_step().getProblem_text();

    List<Object> row = new ArrayList<>();
    row.add(String.valueOf(chatId));
    row.add(abstractText);
    row.add(record.getStr("problem_text"));
    row.add(rewritten);
    row.add(record.getStr("answer_text"));
    row.add(VOTE_LABELS.getOrDefault(record.getStr("vote_status"), "未投票"));
    row.add(paragraphList == null ? 0 : paragraphList.size());
    row.add(paragraphText(paragraphList));
    row.add(improveText(record));
    row.add(asker);
    row.add(number(record, "message_tokens") + number(record, "answer_tokens"));
    row.add(record.getObject("run_time"));
    row.add(time(record.getObject("create_time")));
    return row;
  }

  private String paragraphText(List<ParagraphSearchResultVo> paragraphs) {
    if (paragraphs == null || paragraphs.isEmpty()) {
      return "";
    }
    StringBuilder text = new StringBuilder();
    for (ParagraphSearchResultVo paragraph : paragraphs) {
      if (text.length() > 0) {
        text.append("\n----------\n");
      }
      text.append(paragraph.getTitle() == null ? "" : paragraph.getTitle()).append(":\n").append(paragraph.getContent() == null ? "" : paragraph.getContent());
    }
    return text.toString();
  }

  private String improveText(Row record) {
    List<Long> ids = ChatLogArrays.toLongList(record.get("improve_paragraph_id_list"));
    if (ids.isEmpty()) {
      return "";
    }
    StringBuilder placeholders = new StringBuilder("(");
    for (int i = 0; i < ids.size(); i++) {
      placeholders.append(i == 0 ? "?" : ",?");
    }
    placeholders.append(")");
    List<Row> paragraphs = Db.find("select id,title,content from moss_kb_paragraph where id in " + placeholders, ids.toArray());
    Map<Long, Row> byId = new LinkedHashMap<>();
    for (Row paragraph : paragraphs) {
      byId.put(paragraph.getLong("id"), paragraph);
    }
    StringBuilder text = new StringBuilder();
    for (Long id : ids) {
      Row paragraph = byId.get(id);
      if (paragraph == null) {
        continue;
      }
      if (text.length() > 0) {
        text.append("\n");
      }
      text.append(paragraph.getStr("title") == null ? "" : paragraph.getStr("title")).append("\n").append(paragraph.getStr("content") == null ? "" : paragraph.getStr("content"));
    }
    return text.toString();
  }

  private MossKbChatRecordDetail parseDetail(Object value) {
    String json = null;
    if (value instanceof PGobject pgobject) {
      json = pgobject.getValue();
    } else if (value instanceof String text) {
      json = text;
    }
    if (StrUtil.isBlank(json)) {
      return null;
    }
    return JsonUtils.parse(json, MossKbChatRecordDetail.class);
  }

  /** asker 由 client_id 反查用户，查不到按游客处理。 */
  private String askerName(Row chat) {
    String nickName = chat.getStr("asker_nick_name");
    if (nickName != null && !nickName.isBlank()) {
      return nickName;
    }
    String username = chat.getStr("asker_name");
    return username == null || username.isBlank() ? "游客" : username;
  }

  private int number(Row record, String column) {
    Object value = record.get(column);
    return value instanceof Number number ? number.intValue() : 0;
  }

  private String time(Object value) {
    if (value == null) {
      return "";
    }
    String text = value.toString();
    return text.length() > 19 ? text.substring(0, 19) : text;
  }
}
