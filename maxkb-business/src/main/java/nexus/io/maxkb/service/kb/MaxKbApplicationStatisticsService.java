package nexus.io.maxkb.service.kb;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jfinal.kit.Kv;

import nexus.io.db.activerecord.Db;
import nexus.io.db.activerecord.Row;
import nexus.io.model.result.ResultVo;

/**
 * 应用概览统计:与官方 application_statistics_serializers 对齐。
 *
 * 口径说明:
 * - 问答与 token 取自 max_kb_application_chat_record,按 create_time 左闭右开区间过滤;
 * - 满意度取自同一张表的 vote_status,赞同为 '0'、反对为 '1';
 * - 用户新增取自 max_kb_application_public_access_client,公开访问建立访客记录时写入。
 *
 * 统计值统一用 BigDecimal 输出:
 * 前端 getSum 是字符串拼接语义(total + item),字段一旦是字符串,"提问次数"这类卡片会显示成拼接出来的长数字;
 * 而框架的 JSON 写出为雪花 ID 开了 WriteLongAsString(Long 会写成字符串),所以不能用 Long,BigDecimal 不受影响。
 */
public class MaxKbApplicationStatisticsService {

  /** 对话相关统计趋势:对话统计、满意度、token 与用户新增按天合并。 */
  public ResultVo chatRecordAggregateTrend(Long user, Long applicationId, String startTime, String endTime) {
    if (!ApplicationAccess.owns(user, applicationId)) {
      return ResultVo.fail("无权访问应用统计");
    }
    LocalDate startDate = parseDate(startTime);
    LocalDate endDate = parseDate(endTime);
    ResultVo invalid = validateRange(startDate, endDate);
    if (invalid != null) {
      return invalid;
    }

    List<Map<String, Object>> result = new ArrayList<>();
    LocalDate current = startDate;
    while (!current.isAfter(endDate)) {
      result.add(blankDay(current));
      current = current.plusDays(1);
    }

    merge(result, queryChatRecordTrend(applicationId, startDate, endDate));
    merge(result, queryCustomerAddedTrend(applicationId, startDate, endDate));
    return ResultVo.ok(result);
  }

  /** 对话统计汇总:整个区间一行,额外带区间内新增用户数。 */
  public ResultVo chatRecordAggregate(Long user, Long applicationId, String startTime, String endTime) {
    if (!ApplicationAccess.owns(user, applicationId)) {
      return ResultVo.fail("无权访问应用统计");
    }
    LocalDate startDate = parseDate(startTime);
    LocalDate endDate = parseDate(endTime);
    ResultVo invalid = validateRange(startDate, endDate);
    if (invalid != null) {
      return invalid;
    }
    Map<String, Object> summary = queryChatRecordAggregate(applicationId, startDate, endDate);
    summary.put("customer_added_count", queryCustomerAddedCount(applicationId, startDate, endDate));
    return ResultVo.ok(summary);
  }

  /** 用户统计:区间内新增访客数,并保留官方字段 customer_today_added_count。 */
  public ResultVo customerCount(Long user, Long applicationId, String startTime, String endTime) {
    if (!ApplicationAccess.owns(user, applicationId)) {
      return ResultVo.fail("无权访问应用统计");
    }
    LocalDate startDate = parseDate(startTime);
    LocalDate endDate = parseDate(endTime);
    ResultVo invalid = validateRange(startDate, endDate);
    if (invalid != null) {
      return invalid;
    }

    LocalDate today = LocalDate.now();
    boolean todayInRange = !today.isBefore(startDate) && !today.isAfter(endDate);
    Kv result = Kv.by("customer_added_count", queryCustomerAddedCount(applicationId, startDate, endDate));
    result.set("customer_today_added_count",
        todayInRange ? queryCustomerAddedCount(applicationId, today, today) : BigDecimal.ZERO);
    return ResultVo.ok(result);
  }

  /** 用户统计趋势:区间内按天的新增访客数,只返回有访客的日期。 */
  public ResultVo customerCountTrend(Long user, Long applicationId, String startTime, String endTime) {
    if (!ApplicationAccess.owns(user, applicationId)) {
      return ResultVo.fail("无权访问应用统计");
    }
    LocalDate startDate = parseDate(startTime);
    LocalDate endDate = parseDate(endTime);
    ResultVo invalid = validateRange(startDate, endDate);
    if (invalid != null) {
      return invalid;
    }
    return ResultVo.ok(queryCustomerAddedTrend(applicationId, startDate, endDate));
  }

  private List<Map<String, Object>> queryChatRecordTrend(Long applicationId, LocalDate startDate, LocalDate endDate) {
    List<Row> rows = Db.find(
        "select r.create_time::date as day, count(r.id) as chat_record_count, count(distinct c.client_id) as customer_num,"
            + " coalesce(sum(r.message_tokens + coalesce(r.answer_tokens, 0)), 0) as tokens_num,"
            + " coalesce(sum(case when r.vote_status = '0' then 1 else 0 end), 0) as star_num,"
            + " coalesce(sum(case when r.vote_status = '1' then 1 else 0 end), 0) as trample_num"
            + " from max_kb_application_chat_record r join max_kb_application_chat c on c.id = r.chat_id"
            + " where c.application_id = ? and r.create_time >= ? and r.create_time < ? group by day order by day",
        applicationId, java.sql.Date.valueOf(startDate), java.sql.Date.valueOf(endDate.plusDays(1)));
    List<Map<String, Object>> trend = new ArrayList<>();
    for (Row row : rows) {
      Map<String, Object> item = new HashMap<>();
      item.put("day", String.valueOf(row.getObject("day")));
      item.put("chat_record_count", toNumber(row.getObject("chat_record_count")));
      item.put("customer_num", toNumber(row.getObject("customer_num")));
      item.put("tokens_num", toNumber(row.getObject("tokens_num")));
      item.put("star_num", toNumber(row.getObject("star_num")));
      item.put("trample_num", toNumber(row.getObject("trample_num")));
      trend.add(item);
    }
    return trend;
  }

  private List<Map<String, Object>> queryCustomerAddedTrend(Long applicationId, LocalDate startDate, LocalDate endDate) {
    List<Row> rows = Db.find(
        "select p.create_time::date as day, count(p.id) as customer_added_count"
            + " from max_kb_application_public_access_client p"
            + " where p.application_id = ? and p.create_time >= ? and p.create_time < ? group by day order by day",
        applicationId, java.sql.Date.valueOf(startDate), java.sql.Date.valueOf(endDate.plusDays(1)));
    List<Map<String, Object>> trend = new ArrayList<>();
    for (Row row : rows) {
      Map<String, Object> item = new HashMap<>();
      item.put("day", String.valueOf(row.getObject("day")));
      item.put("customer_added_count", toNumber(row.getObject("customer_added_count")));
      trend.add(item);
    }
    return trend;
  }

  private Map<String, Object> queryChatRecordAggregate(Long applicationId, LocalDate startDate, LocalDate endDate) {
    Row row = Db.findFirst(
        "select coalesce(sum(case when r.vote_status = '0' then 1 else 0 end), 0) as star_num,"
            + " coalesce(sum(case when r.vote_status = '1' then 1 else 0 end), 0) as trample_num,"
            + " coalesce(sum(r.message_tokens + coalesce(r.answer_tokens, 0)), 0) as tokens_num,"
            + " count(distinct c.client_id) as customer_num, count(r.id) as chat_record_count"
            + " from max_kb_application_chat_record r join max_kb_application_chat c on c.id = r.chat_id"
            + " where c.application_id = ? and r.create_time >= ? and r.create_time < ?",
        applicationId, java.sql.Date.valueOf(startDate), java.sql.Date.valueOf(endDate.plusDays(1)));
    Map<String, Object> summary = new HashMap<>();
    summary.put("star_num", row == null ? BigDecimal.ZERO : toNumber(row.getObject("star_num")));
    summary.put("trample_num", row == null ? BigDecimal.ZERO : toNumber(row.getObject("trample_num")));
    summary.put("tokens_num", row == null ? BigDecimal.ZERO : toNumber(row.getObject("tokens_num")));
    summary.put("customer_num", row == null ? BigDecimal.ZERO : toNumber(row.getObject("customer_num")));
    summary.put("chat_record_count", row == null ? BigDecimal.ZERO : toNumber(row.getObject("chat_record_count")));
    return summary;
  }

  private BigDecimal queryCustomerAddedCount(Long applicationId, LocalDate startDate, LocalDate endDate) {
    Long count = Db.queryLong(
        "select count(*) from max_kb_application_public_access_client p"
            + " where p.application_id = ? and p.create_time >= ? and p.create_time < ?",
        applicationId, java.sql.Date.valueOf(startDate), java.sql.Date.valueOf(endDate.plusDays(1)));
    return count == null ? BigDecimal.ZERO : BigDecimal.valueOf(count);
  }

  private Map<String, Object> blankDay(LocalDate day) {
    Map<String, Object> item = new HashMap<>();
    item.put("day", day.toString());
    item.put("chat_record_count", BigDecimal.ZERO);
    item.put("customer_num", BigDecimal.ZERO);
    item.put("tokens_num", BigDecimal.ZERO);
    item.put("star_num", BigDecimal.ZERO);
    item.put("trample_num", BigDecimal.ZERO);
    item.put("customer_added_count", BigDecimal.ZERO);
    return item;
  }

  private void merge(List<Map<String, Object>> target, List<Map<String, Object>> source) {
    for (Map<String, Object> item : source) {
      String day = String.valueOf(item.get("day"));
      for (Map<String, Object> exist : target) {
        if (day.equals(exist.get("day"))) {
          exist.putAll(item);
          break;
        }
      }
    }
  }

  private ResultVo validateRange(LocalDate startDate, LocalDate endDate) {
    if (startDate == null || endDate == null) {
      return ResultVo.fail("开始时间或结束时间格式不正确");
    }
    if (startDate.isAfter(endDate)) {
      return ResultVo.fail("开始时间不能晚于结束时间");
    }
    return null;
  }

  private LocalDate parseDate(String value) {
    try {
      return value == null ? null : LocalDate.parse(value);
    } catch (RuntimeException e) {
      return null;
    }
  }

  /** 统计值统一输出为 JSON 数字,前端 getSum 依赖数字而不是字符串。 */
  private BigDecimal toNumber(Object value) {
    if (value instanceof Number) {
      return new BigDecimal(value.toString());
    }
    try {
      return value == null || "null".equals(value) ? BigDecimal.ZERO : new BigDecimal(value.toString().trim());
    } catch (RuntimeException e) {
      return BigDecimal.ZERO;
    }
  }
}
