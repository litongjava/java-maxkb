package nexus.io.maxkb.service.kb;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import com.jfinal.kit.Kv;

import nexus.io.db.activerecord.Db;
import nexus.io.db.activerecord.Row;
import nexus.io.jfinal.aop.Aop;
import nexus.io.model.result.ResultVo;

/**
 * 对话日志读取：会话分页、问答分页、标题、删除与评价。
 *
 * 会话列表的口径与官方前端一致：提问数、赞同数、反对数、标注数与会话 token 合计在 SQL 内一次算出；
 * asker 由 client_id 反查用户，查不到时按游客显示（分享访客的 client_id 不对应后台账号）。
 */
public class MaxKbChatHistoryService {

  /** 会话列表查询条件，来自界面的时间区间、标题搜索与评价筛选。 */
  public static class Query {
    public String abstractText;
    public String startTime;
    public String endTime;
    public Integer minStar;
    public Integer minTrample;
    public String comparer;
    public List<Long> selectIds;

    /** 自由文本搜索：标题包含关键字，命中不区分大小写。 */
    boolean hasAbstract() {
      return abstractText != null && !abstractText.isBlank();
    }

    boolean hasVoteFilter() {
      return minStar != null || minTrample != null;
    }

    boolean isOrComparer() {
      return "or".equalsIgnoreCase(comparer);
    }
  }

  public ResultVo chats(Long user, Long application, int page, int size, boolean clientOnly) {
    return chats(user, application, page, size, clientOnly, new Query());
  }

  public ResultVo chats(Long user, Long application, int page, int size, boolean clientOnly, Query query) {
    if (clientOnly ? !ApplicationAccess.canChat(user, application) : !ApplicationAccess.owns(user, application)) {
      return ResultVo.fail("无权访问对话日志");
    }
    page = Math.max(1, page);
    size = Math.max(1, Math.min(100, size));
    Sql sql = listSql(application, clientOnly ? user : null, query, false);
    long total = Db.queryLong("select count(*) from (" + sql.select + ") chat_log", sql.args.toArray());
    List<Object> pageArgs = new ArrayList<>(sql.args);
    pageArgs.add(size);
    pageArgs.add((page - 1) * size);
    List<Row> rows = Db.find(sql.select + " order by c.update_time desc,c.id desc limit ? offset ?", pageArgs.toArray());
    List<Kv> records = new ArrayList<>();
    for (Row row : rows) {
      records.add(toChat(row));
    }
    return ResultVo.ok(Kv.by("current", page).set("size", size).set("total", total).set("records", records));
  }

  /** 导出复用的会话行查询，select_ids 非空时只取勾选的会话。 */
  List<Row> chatsForExport(Long user, Long application, Query query) {
    Sql sql = listSql(application, null, query, true);
    return Db.find(sql.select + " order by c.update_time desc,c.id desc", sql.args.toArray());
  }

  public ResultVo records(Long user, Long application, Long chat, int page, int size, boolean ascending) {
    if (!ApplicationAccess.canReadChat(user, application, chat)) {
      return ResultVo.fail("无权访问会话");
    }
    page = Math.max(1, page);
    size = Math.max(1, Math.min(100, size));
    long total = Db.queryLong("select count(*) from max_kb_application_chat_record where chat_id=?", chat);
    List<Long> ids = Db.queryListLong("select id from max_kb_application_chat_record where chat_id=? order by id " + (ascending ? "asc" : "desc") + " limit ? offset ?", chat, size, (page - 1) * size);
    List<Object> records = new ArrayList<>();
    for (Long id : ids) {
      records.add(Aop.get(MaxKbApplicationCharRecordService.class).get(user, application, chat, id).getData());
    }
    return ResultVo.ok(Kv.by("current", page).set("size", size).set("total", total).set("records", records));
  }

  public ResultVo rename(Long user, Long application, Long chat, String title) {
    if (!ApplicationAccess.canReadChat(user, application, chat)) {
      return ResultVo.fail("无权访问会话");
    }
    if (title == null || title.isBlank() || title.length() > 256) {
      return ResultVo.fail("会话标题需要1至256个字符");
    }
    Db.update("update max_kb_application_chat set abstract=?,update_time=now() where id=?", title, chat);
    return ResultVo.ok(true);
  }

  public ResultVo remove(Long user, Long application, Long chat) {
    if (!ApplicationAccess.canReadChat(user, application, chat)) {
      return ResultVo.fail("无权访问会话");
    }
    Db.update("update max_kb_application_chat set is_deleted=true where id=?", chat);
    return ResultVo.ok(true);
  }

  public ResultVo vote(Long user, Long application, Long chat, Long record, String vote) {
    if (!ApplicationAccess.canReadChat(user, application, chat) || vote == null || !List.of("-1", "0", "1").contains(vote)) {
      return ResultVo.fail("无效的评价请求");
    }
    boolean updated = Db.update("update max_kb_application_chat_record set vote_status=? where id=? and chat_id=?", vote, record, chat) > 0;
    return ResultVo.ok(updated);
  }

  /** 会话行的界面字段：统计值补齐为 0，asker 统一为对象。 */
  private Kv toChat(Row row) {
    Kv value = row.toKv();
    value.set("chat_record_count", number(row, "chat_record_count"));
    value.set("star_num", number(row, "star_num"));
    value.set("trample_num", number(row, "trample_num"));
    value.set("mark_sum", number(row, "mark_sum"));
    value.set("tokens_num", number(row, "tokens_num"));
    String nickName = row.getStr("asker_nick_name");
    String username = row.getStr("asker_name");
    String userName = nickName != null && !nickName.isBlank() ? nickName : (username != null && !username.isBlank() ? username : "游客");
    value.set("asker", Kv.by("user_name", userName));
    value.remove("asker_nick_name");
    value.remove("asker_name");
    value.set("create_time", java.util.Objects.toString(row.getObject("create_time")));
    value.set("update_time", java.util.Objects.toString(row.getObject("update_time")));
    return value;
  }

  private Object number(Row row, String column) {
    Object value = row.get(column);
    return value == null ? 0 : value;
  }

  private static class Sql {
    String select;
    List<Object> args;
  }

  /**
   * 会话统计子查询 + 用户反查 + 条件拼装。
   *
   * @param clientId 分享端只取该客户端的会话；管理端传 null
   * @param export   导出时按 select_ids 收窄
   */
  private Sql listSql(Long application, Long clientId, Query query, boolean export) {
    StringBuilder select = new StringBuilder();
    select.append("select c.*, coalesce(s.chat_record_count,0) chat_record_count, coalesce(s.star_num,0) star_num,")
        .append(" coalesce(s.trample_num,0) trample_num, coalesce(s.mark_sum,0) mark_sum, coalesce(s.tokens_num,0) tokens_num,")
        .append(" u.username asker_name, u.nick_name asker_nick_name")
        .append(" from max_kb_application_chat c")
        .append(" left join (select r.chat_id, count(*) chat_record_count,")
        .append(" sum(case when r.vote_status='0' then 1 else 0 end) star_num,")
        .append(" sum(case when r.vote_status='1' then 1 else 0 end) trample_num,")
        .append(" sum(case when r.improve_paragraph_id_list is null then 0 else array_length(r.improve_paragraph_id_list,1) end) mark_sum,")
        .append(" sum(coalesce(r.answer_tokens,0)+coalesce(r.message_tokens,0)) tokens_num")
        .append(" from max_kb_application_chat_record r group by r.chat_id) s on s.chat_id=c.id")
        .append(" left join max_kb_user u on u.id=c.client_id");
    List<Object> args = new ArrayList<>();
    select.append(" where c.application_id=? and c.is_deleted=false");
    args.add(application);
    if (clientId != null) {
      select.append(" and c.client_id=? and c.chat_type=0");
      args.add(clientId);
    }
    if (query.hasAbstract()) {
      select.append(" and c.abstract ilike ?");
      args.add("%" + query.abstractText + "%");
    }
    OffsetDateTime from = startOfDay(query.startTime);
    if (from != null) {
      select.append(" and c.update_time >= ?");
      args.add(from);
    }
    OffsetDateTime to = endOfDay(query.endTime);
    if (to != null) {
      select.append(" and c.update_time <= ?");
      args.add(to);
    }
    if (query.hasVoteFilter()) {
      String star = "coalesce(s.star_num,0) >= ?";
      String trample = "coalesce(s.trample_num,0) >= ?";
      if (query.minStar != null && query.minTrample != null) {
        select.append(" and (").append(star).append(query.isOrComparer() ? " or " : " and ").append(trample).append(")");
        args.add(query.minStar);
        args.add(query.minTrample);
      } else if (query.minStar != null) {
        select.append(" and ").append(star);
        args.add(query.minStar);
      } else {
        select.append(" and ").append(trample);
        args.add(query.minTrample);
      }
    }
    if (export && query.selectIds != null && !query.selectIds.isEmpty()) {
      select.append(" and c.id in (");
      for (int i = 0; i < query.selectIds.size(); i++) {
        select.append(i == 0 ? "?" : ",?");
        args.add(query.selectIds.get(i));
      }
      select.append(")");
    }
    Sql sql = new Sql();
    sql.select = select.toString();
    sql.args = args;
    return sql;
  }

  /** 起止日期按本机时区的整天处理，与界面日期选择器一致；非法日期直接忽略该条件。 */
  static OffsetDateTime startOfDay(String date) {
    LocalDate value = parseDate(date);
    return value == null ? null : value.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
  }

  static OffsetDateTime endOfDay(String date) {
    LocalDate value = parseDate(date);
    return value == null ? null : value.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusNanos(1).toOffsetDateTime();
  }

  private static LocalDate parseDate(String date) {
    if (date == null || date.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(date.trim());
    } catch (java.time.format.DateTimeParseException e) {
      return null;
    }
  }
}
