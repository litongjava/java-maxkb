package nexus.io.mosskb.service.kb;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.alibaba.fastjson2.JSONObject;
import com.jfinal.kit.Kv;

import nexus.io.db.activerecord.Db;
import nexus.io.db.activerecord.Row;
import nexus.io.jfinal.aop.Aop;
import nexus.io.mosskb.constant.MossKbTableNames;
import nexus.io.model.result.ResultVo;
import nexus.io.tio.utils.snowflake.SnowflakeIdUtils;

/**
 * 对话日志的标注与「加入知识库」。
 *
 * 一轮问答被标注后，答案会作为新段落写入所选文档，并把自己的段落 ID 追加到
 * moss_kb_application_chat_record.improve_paragraph_id_list；删除标注时同时移除段落与关联问题。
 * 应用、会话、记录、知识库与文档五者都要通过归属检查，避免跨应用或跨知识库写入。
 */
public class MossKbChatLogService {

  /** 把一条问答的答案标注进知识库，返回更新后的记录详情。 */
  public ResultVo improve(Long userId, Long applicationId, Long chatId, Long recordId, Long datasetId, Long documentId, JSONObject body) {
    ResultVo denied = check(userId, applicationId, chatId, recordId, datasetId, documentId);
    if (denied != null) {
      return denied;
    }
    String content = body == null ? null : body.getString("content");
    if (content == null || content.isBlank()) {
      return ResultVo.fail("段落内容不能为空");
    }
    if (content.length() > 100000) {
      return ResultVo.fail("段落内容不能超过 100000 个字符");
    }
    String title = body.getString("title");
    String problemText = body.getString("problem_text");

    Long paragraphId = Aop.get(MossKbParagraphServcie.class).insertParagraph(datasetId, documentId, title, content);
    saveProblem(datasetId, documentId, paragraphId, problemText, recordId);
    appendParagraph(recordId, chatId, paragraphId);
    return Aop.get(MossKbApplicationCharRecordService.class).get(userId, applicationId, chatId, recordId);
  }

  /** 当前记录已标注的段落列表，供界面查看与编辑。 */
  public ResultVo listImprove(Long userId, Long applicationId, Long chatId, Long recordId) {
    if (!ApplicationAccess.canReadChat(userId, applicationId, chatId)) {
      return ResultVo.fail("会话不存在或无权访问");
    }
    Row record = Db.findFirst("select improve_paragraph_id_list from moss_kb_application_chat_record where id=? and chat_id=?", recordId, chatId);
    if (record == null) {
      return ResultVo.fail("对话记录不存在");
    }
    List<Long> paragraphIds = ChatLogArrays.toLongList(record.get("improve_paragraph_id_list"));
    if (paragraphIds.isEmpty()) {
      return ResultVo.ok(new ArrayList<>());
    }
    List<Row> paragraphs = Db.find("select id,title,content,dataset_id,document_id from moss_kb_paragraph where id in " + placeholders(paragraphIds.size()), paragraphIds.toArray());
    List<Kv> result = new ArrayList<>();
    Set<Long> existing = new LinkedHashSet<>();
    for (Row paragraph : paragraphs) {
      Long id = paragraph.getLong("id");
      existing.add(id);
      result.add(Kv.by("id", id)
          .set("title", paragraph.getStr("title"))
          .set("content", paragraph.getStr("content"))
          .set("dataset", paragraph.getLong("dataset_id"))
          .set("document", paragraph.getLong("document_id"))
          .set("chat_id", chatId));
    }
    if (existing.size() != paragraphIds.size()) {
      // 段落被单独删除后清理残留 ID，界面下次读取即为一致状态
      Db.update("update moss_kb_application_chat_record set improve_paragraph_id_list=?::bigint[] where id=? and chat_id=?",
          "{" + join(existing) + "}", recordId, chatId);
    }
    return ResultVo.ok(result);
  }

  /** 删除一条标注：段落、问题关联以及记录上的段落 ID 一起移除。 */
  public ResultVo deleteImprove(Long userId, Long applicationId, Long chatId, Long recordId, Long datasetId, Long documentId, Long paragraphId) {
    ResultVo denied = check(userId, applicationId, chatId, recordId, datasetId, documentId);
    if (denied != null) {
      return denied;
    }
    Row record = Db.findFirst("select improve_paragraph_id_list from moss_kb_application_chat_record where id=? and chat_id=?", recordId, chatId);
    List<Long> paragraphIds = record == null ? List.of() : ChatLogArrays.toLongList(record.get("improve_paragraph_id_list"));
    if (!paragraphIds.contains(paragraphId)) {
      return ResultVo.fail("该记录没有这个标注段落");
    }
    Db.update("update moss_kb_application_chat_record set improve_paragraph_id_list=array_remove(improve_paragraph_id_list,?::bigint) where id=? and chat_id=?", paragraphId, recordId, chatId);
    Aop.get(MossKbParagraphServcie.class).deleteParagraph(datasetId, documentId, paragraphId);
    return ResultVo.ok(true);
  }

  /** 把多条会话记录批量加入知识库：问题作为段落标题，答案作为段落内容。 */
  public ResultVo improveBatch(Long userId, Long applicationId, Long datasetId, JSONObject body) {
    if (!ApplicationAccess.owns(userId, applicationId)) {
      return ResultVo.fail("应用不存在或无权访问");
    }
    if (!DatasetAccess.owns(userId, datasetId)) {
      return ResultVo.fail("知识库不存在或无权访问");
    }
    Long documentId = body == null ? null : body.getLong("document_id");
    if (!DatasetAccess.ownsDocument(userId, datasetId, documentId)) {
      return ResultVo.fail("文档不存在或无权访问");
    }
    List<Long> chatIds = new ArrayList<>();
    if (body != null && body.getJSONArray("chat_ids") != null) {
      for (Object value : body.getJSONArray("chat_ids")) {
        if (value != null) {
          chatIds.add(Long.valueOf(value.toString()));
        }
      }
    }
    if (chatIds.isEmpty()) {
      return ResultVo.fail("请选择要加入知识库的对话");
    }

    List<Long> paragraphIds = new ArrayList<>();
    MossKbParagraphServcie paragraphService = Aop.get(MossKbParagraphServcie.class);
    for (Long chatId : chatIds) {
      if (!ApplicationAccess.canReadChat(userId, applicationId, chatId)) {
        return ResultVo.fail("会话不存在或无权访问");
      }
      List<Row> records = Db.find("select id,problem_text,answer_text from moss_kb_application_chat_record where chat_id=? order by id", chatId);
      for (Row record : records) {
        String answer = record.getStr("answer_text");
        if (answer == null || answer.isBlank()) {
          continue;
        }
        String problem = record.getStr("problem_text");
        Long paragraphId = paragraphService.insertParagraph(datasetId, documentId, problem, answer);
        saveProblem(datasetId, documentId, paragraphId, problem, record.getLong("id"));
        appendParagraph(record.getLong("id"), chatId, paragraphId);
        paragraphIds.add(paragraphId);
      }
    }
    return ResultVo.ok(Kv.by("paragraph_ids", paragraphIds).set("dataset_id", datasetId));
  }

  /** 应用、会话、记录、知识库与文档的归属校验。 */
  private ResultVo check(Long userId, Long applicationId, Long chatId, Long recordId, Long datasetId, Long documentId) {
    if (!ApplicationAccess.canReadChat(userId, applicationId, chatId)) {
      return ResultVo.fail("会话不存在或无权访问");
    }
    if (Db.queryLong("select count(*) from moss_kb_application_chat_record where id=? and chat_id=?", recordId, chatId) == 0) {
      return ResultVo.fail("对话记录不存在");
    }
    if (!DatasetAccess.owns(userId, datasetId)) {
      return ResultVo.fail("知识库不存在或无权访问");
    }
    if (!DatasetAccess.ownsDocument(userId, datasetId, documentId)) {
      return ResultVo.fail("文档不存在或无权访问");
    }
    return null;
  }

  /** 问题与段落的关联，问题内容相同则复用已有问题。 */
  private void saveProblem(Long datasetId, Long documentId, Long paragraphId, String problemText, Long recordId) {
    String content = problemText;
    if (content == null || content.isBlank()) {
      Row record = Db.findFirst("select problem_text from moss_kb_application_chat_record where id=?", recordId);
      content = record == null ? null : record.getStr("problem_text");
    }
    if (content == null || content.isBlank()) {
      return;
    }
    Long problemId = Db.queryLong("select id from moss_kb_problem where dataset_id=? and content=? and deleted=0 limit 1", datasetId, content);
    if (problemId == null) {
      problemId = SnowflakeIdUtils.id();
      Db.save(MossKbTableNames.moss_kb_problem, Row.by("id", problemId).set("dataset_id", datasetId).set("hit_num", 0).set("content", content));
    }
    Db.save(MossKbTableNames.moss_kb_problem_paragraph_mapping, Row.by("id", SnowflakeIdUtils.id())
        .set("dataset_id", datasetId).set("document_id", documentId).set("paragraph_id", paragraphId).set("problem_id", problemId));
  }

  private void appendParagraph(Long recordId, Long chatId, Long paragraphId) {
    Db.update("update moss_kb_application_chat_record set improve_paragraph_id_list=array_append(coalesce(improve_paragraph_id_list,'{}'),?::bigint) where id=? and chat_id=?",
        paragraphId, recordId, chatId);
  }

  private String placeholders(int size) {
    StringBuilder sql = new StringBuilder("(");
    for (int i = 0; i < size; i++) {
      sql.append(i == 0 ? "?" : ",?");
    }
    return sql.append(")").toString();
  }

  private String join(Set<Long> ids) {
    StringBuilder text = new StringBuilder();
    for (Long id : ids) {
      if (text.length() > 0) {
        text.append(",");
      }
      text.append(id);
    }
    return text.toString();
  }
}
