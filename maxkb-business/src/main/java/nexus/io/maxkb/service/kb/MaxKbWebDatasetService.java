package nexus.io.maxkb.service.kb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.postgresql.util.PGobject;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jfinal.kit.Kv;

import lombok.extern.slf4j.Slf4j;
import nexus.io.db.activerecord.Db;
import nexus.io.db.activerecord.Row;
import nexus.io.jfinal.aop.Aop;
import nexus.io.kit.PgObjectUtils;
import nexus.io.maxkb.constant.MaxKbTableNames;
import nexus.io.maxkb.utils.ExecutorServiceUtils;
import nexus.io.maxkb.utils.JsonColumnUtils;
import nexus.io.maxkb.vo.KbDatasetModel;
import nexus.io.model.result.ResultVo;
import nexus.io.tio.utils.crypto.Md5Utils;
import nexus.io.tio.utils.snowflake.SnowflakeIdUtils;

/**
 * Web 站点知识库：一个根地址加一个可选的正文选择器，抓取结果按页面写入文档。
 *
 * <p>
 * 创建知识库时会先真实访问一次根地址，地址不可用时直接失败，不会留下空知识库。
 * 页面抓取与向量化在后台线程执行，接口立即返回，文档随后出现在文档列表里。
 */
@Slf4j
public class MaxKbWebDatasetService {

  /** 知识库类型：1 表示 Web 站点。 */
  public static final String DATASET_TYPE_WEB = "1";

  /** 文档类型：1 表示由 Web 站点抓取生成。 */
  public static final String DOCUMENT_TYPE_WEB = "1";

  private static final int MAX_NAME_LENGTH = 64;

  private static final int MAX_DESC_LENGTH = 256;

  /** 一次导入或批量同步的网页数量上限。 */
  private static final int MAX_SOURCE_URLS = 100;

  /**
   * 正在抓取的知识库与正在同步的文档。
   *
   * <p>
   * 同一个知识库同时跑两个抓取任务会写出重复文档，整体同步与抓取任务也可能互相覆盖，
   * 因此每个知识库、每个文档同一时间只允许一个任务。多实例部署时需要换成分布式锁。
   */
  private static final Set<String> RUNNING = ConcurrentHashMap.newKeySet();

  private static final String DATASET_KEY = "dataset:";

  private static final String DOCUMENT_KEY = "document:";

  /** 同一个地址只对应一个文档，替换同步据此复用已有文档。 */
  private static final String FIND_DOCUMENT_BY_SOURCE_URL = """
      select id
        from max_kb_document
       where dataset_id = ?
         and meta->>'source_url' = ?
      """;

  /** 同步失败时只改状态，保留已有分段，用户可以直接重试。 */
  private static final String MARK_DOCUMENT_FAILED = """
      update max_kb_document
         set status = ?, update_time = now()
       where id = ?
      """;

  /** 重新抓取一个文档时整份替换它的分段。 */
  private static final String DELETE_DOCUMENT_PARAGRAPHS = """
      delete from max_kb_paragraph
       where document_id = ?
      """;

  private static final String DELETE_PROBLEM_PARAGRAPH_MAPPING = """
      delete from max_kb_problem_paragraph_mapping
       where dataset_id = ?
      """;

  private static final String DELETE_DATASET_PARAGRAPHS = """
      delete from max_kb_paragraph
       where dataset_id = ?
      """;

  private static final String DELETE_DATASET_DOCUMENTS = """
      delete from max_kb_document
       where dataset_id = ?
      """;

  /**
   * 创建 Web 站点知识库。
   *
   * <p>
   * 请求体字段：name、desc、embedding_mode_id、source_url、selector。
   */
  public ResultVo save(Long userId, JSONObject input) {
    String name = text(input.getString("name"));
    if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
      return ResultVo.fail("知识库名称必填，且不能超过 64 个字符");
    }
    String desc = text(input.getString("desc"));
    if (desc.isEmpty()) {
      return ResultVo.fail("知识库描述必填");
    }
    if (desc.length() > MAX_DESC_LENGTH) {
      return ResultVo.fail("知识库描述不能超过 256 个字符");
    }
    Long embeddingModeId = input.getLong("embedding_mode_id");
    if (embeddingModeId == null) {
      return ResultVo.fail("请选择向量模型");
    }
    String sourceUrl = WebCrawlService.absoluteUrl(text(input.getString("source_url")));
    if (sourceUrl == null) {
      return ResultVo.fail("Web 根地址必须是 http 或 https 地址");
    }
    String selector = text(input.getString("selector"));
    if (Db.queryLong("select count(*) from max_kb_dataset where user_id=? and name=? and deleted=0", userId, name) > 0) {
      return ResultVo.fail("知识库名称已存在");
    }
    // 创建前先抓一次入口页面：地址不可用或正文取不到内容时立即返回错误。
    WebCrawlService crawler = new WebCrawlService();
    WebCrawlService.Page firstPage;
    try {
      firstPage = crawler.fetch(sourceUrl, selector);
    } catch (IllegalArgumentException e) {
      return ResultVo.fail(e.getMessage());
    } catch (IOException e) {
      return ResultVo.fail("Web 根地址不可用：" + e.getMessage().replace("无法访问该地址：", ""));
    }
    if (crawler.split(firstPage.markdown()).isEmpty()) {
      return ResultVo.fail("未能从该地址提取到正文，请检查地址或选择器");
    }

    KbDatasetModel model = new KbDatasetModel();
    model.setName(name).setDesc(desc).setType(DATASET_TYPE_WEB).setEmbedding_mode_id(embeddingModeId)
        .setMeta(JSONObject.of("source_url", sourceUrl, "selector", selector, "embedding_mode_id", embeddingModeId));
    ResultVo saved = Aop.get(MaxKbDatasetService.class).save(userId, model);
    if (saved.getCode() != 200) {
      return saved;
    }
    Long datasetId = Long.valueOf(String.valueOf(((Kv) saved.getData()).get("id")));
    Long owner = Db.queryLong("select user_id from max_kb_dataset where id=?", datasetId);
    String key = DATASET_KEY + datasetId;
    acquire(key);
    submit(key, () -> crawlDataset(datasetId, owner, embeddingModeId, sourceUrl, selector, firstPage));
    return ResultVo.ok(createResponse(datasetId));
  }

  /** 同步整个 Web 站点知识库：replace 保留未抓到的旧文档，complete 先清空再抓取。 */
  public ResultVo sync(Long userId, Long datasetId, String syncType) {
    Row dataset = dataset(userId, datasetId);
    if (dataset == null) {
      return ResultVo.fail("知识库不存在或无权访问");
    }
    if (!DATASET_TYPE_WEB.equals(dataset.getStr("type"))) {
      return ResultVo.fail("只有 Web 站点知识库支持同步");
    }
    if (!"replace".equals(syncType) && !"complete".equals(syncType)) {
      return ResultVo.fail("同步方式只支持 replace 或 complete");
    }
    JSONObject meta = JsonColumnUtils.toJsonObject(dataset.get("meta"));
    String sourceUrl = WebCrawlService.absoluteUrl(meta == null ? null : meta.getString("source_url"));
    if (sourceUrl == null) {
      return ResultVo.fail("知识库缺少 Web 根地址，无法同步");
    }
    String selector = meta == null ? "" : text(meta.getString("selector"));
    Long embeddingModeId = dataset.getLong("embedding_mode_id");
    Long owner = dataset.getLong("user_id");
    String key = DATASET_KEY + datasetId;
    if (!acquire(key)) {
      return ResultVo.fail("该知识库正在同步中，请稍后再试");
    }
    try {
      if ("complete".equals(syncType)) {
        Db.tx(() -> {
          deleteDocuments(datasetId);
          return true;
        });
      }
      submit(key, () -> crawlDataset(datasetId, owner, embeddingModeId, sourceUrl, selector, null));
    } catch (RuntimeException e) {
      release(key);
      throw e;
    }
    return ResultVo.ok();
  }

  /** 按地址列表导入网页文档，每个地址生成一个文档。 */
  public ResultVo importDocuments(Long userId, Long datasetId, JSONObject input) {
    Row dataset = dataset(userId, datasetId);
    if (dataset == null) {
      return ResultVo.fail("知识库不存在或无权访问");
    }
    if (!DATASET_TYPE_WEB.equals(dataset.getStr("type"))) {
      return ResultVo.fail("只有 Web 站点知识库支持导入网页文档");
    }
    JSONArray urls = input.getJSONArray("source_url_list");
    if (urls == null || urls.isEmpty()) {
      return ResultVo.fail("至少需要一个网页地址");
    }
    if (urls.size() > MAX_SOURCE_URLS) {
      return ResultVo.fail("一次最多导入 " + MAX_SOURCE_URLS + " 个网页地址");
    }
    List<String> targets = new ArrayList<>();
    Set<String> unique = new LinkedHashSet<>();
    for (int i = 0; i < urls.size(); i++) {
      String url = WebCrawlService.absoluteUrl(urls.getString(i));
      if (url == null) {
        return ResultVo.fail("网页地址必须是 http 或 https 地址：" + text(urls.getString(i)));
      }
      if (unique.add(url)) {
        targets.add(url);
      }
    }
    String selector = text(input.getString("selector"));
    Long embeddingModeId = dataset.getLong("embedding_mode_id");
    Long owner = dataset.getLong("user_id");
    String key = DATASET_KEY + datasetId;
    if (!acquire(key)) {
      return ResultVo.fail("该知识库正在同步中，请稍后再试");
    }
    try {
      submit(key, () -> {
        WebCrawlService crawler = new WebCrawlService();
        for (String url : targets) {
          importPage(crawler, datasetId, owner, embeddingModeId, selector, url);
        }
      });
    } catch (RuntimeException e) {
      release(key);
      throw e;
    }
    return ResultVo.ok(Kv.by("count", targets.size()));
  }

  /** 重新抓取一个网页文档，段落整体替换。 */
  public ResultVo syncDocument(Long userId, Long datasetId, Long documentId) {
    if (!DatasetAccess.ownsDocument(userId, datasetId, documentId)) {
      return ResultVo.fail("文档不存在或无权访问");
    }
    Row document = Db.findById(MaxKbTableNames.max_kb_document, documentId);
    if (!DOCUMENT_TYPE_WEB.equals(document.getStr("type"))) {
      return ResultVo.fail("只有 Web 站点文档支持同步");
    }
    JSONObject meta = JsonColumnUtils.toJsonObject(document.get("meta"));
    String sourceUrl = WebCrawlService.absoluteUrl(meta == null ? null : meta.getString("source_url"));
    if (sourceUrl == null) {
      return ResultVo.fail("该文档没有来源地址，无法同步");
    }
    String selector = meta.getString("selector") == null ? "" : text(meta.getString("selector"));
    Row dataset = Db.findFirst("select user_id, embedding_mode_id from max_kb_dataset where id=?", datasetId);
    Long embeddingModeId = dataset.getLong("embedding_mode_id");
    Long owner = dataset.getLong("user_id");
    String key = DOCUMENT_KEY + documentId;
    if (!acquire(key)) {
      return ResultVo.fail("该文档正在同步中，请稍后再试");
    }
    try {
      submit(key, () -> syncPage(new WebCrawlService(), datasetId, owner, embeddingModeId, selector, sourceUrl, documentId));
    } catch (RuntimeException e) {
      release(key);
      throw e;
    }
    return ResultVo.ok();
  }

  /** 批量同步网页文档。 */
  public ResultVo batchSyncDocuments(Long userId, Long datasetId, JSONArray idList) {
    if (!DatasetAccess.owns(userId, datasetId)) {
      return ResultVo.fail("知识库不存在或无权访问");
    }
    if (idList == null || idList.isEmpty()) {
      return ResultVo.fail("请选择需要同步的文档");
    }
    if (idList.size() > MAX_SOURCE_URLS) {
      return ResultVo.fail("一次最多同步 " + MAX_SOURCE_URLS + " 个文档");
    }
    Row dataset = Db.findFirst("select user_id, embedding_mode_id from max_kb_dataset where id=?", datasetId);
    Long embeddingModeId = dataset.getLong("embedding_mode_id");
    Long owner = dataset.getLong("user_id");
    List<Long> documentIds = new ArrayList<>();
    for (int i = 0; i < idList.size(); i++) {
      documentIds.add(idList.getLong(i));
    }
    submit(() -> {
      WebCrawlService crawler = new WebCrawlService();
      for (Long documentId : documentIds) {
        String key = DOCUMENT_KEY + documentId;
        if (!acquire(key)) {
          continue;
        }
        try {
          Row document = Db.findById(MaxKbTableNames.max_kb_document, documentId);
          if (document == null || !DOCUMENT_TYPE_WEB.equals(document.getStr("type"))) {
            continue;
          }
          JSONObject meta = JsonColumnUtils.toJsonObject(document.get("meta"));
          String sourceUrl = WebCrawlService.absoluteUrl(meta == null ? null : meta.getString("source_url"));
          if (sourceUrl == null) {
            continue;
          }
          String selector = meta.getString("selector") == null ? "" : text(meta.getString("selector"));
          syncPage(crawler, datasetId, owner, embeddingModeId, selector, sourceUrl, documentId);
        } finally {
          release(key);
        }
      }
    });
    return ResultVo.ok();
  }

  /** 抓取整个站点：入口页面已经抓过时直接复用，其余页面按同目录前缀展开。 */
  private void crawlDataset(Long datasetId, Long userId, Long embeddingModeId, String sourceUrl, String selector,
      WebCrawlService.Page firstPage) {
    WebCrawlService crawler = new WebCrawlService();
    int count;
    try {
      count = crawler.crawl(sourceUrl, selector, firstPage, page -> {
        try {
          int paragraphs = saveDocument(crawler, datasetId, userId, embeddingModeId, selector, page,
              findDocumentId(datasetId, page.url()));
          log.info("保存网页文档 datasetId={} url={} paragraphs={}", datasetId, page.url(), paragraphs);
        } catch (Exception e) {
          log.warn("保存网页文档失败 datasetId={} url={} message={}", datasetId, page.url(), e.getMessage());
        }
      });
    } catch (Exception e) {
      log.warn("同步 Web 知识库失败 datasetId={} message={}", datasetId, e.getMessage());
      return;
    }
    log.info("Web 知识库同步完成 datasetId={} pages={}", datasetId, count);
  }

  /** 抓取单个地址；失败时留下一条可见的失败文档，便于用户重试。 */
  private void importPage(WebCrawlService crawler, Long datasetId, Long userId, Long embeddingModeId, String selector,
      String url) {
    try {
      WebCrawlService.Page page = crawler.fetch(url, selector);
      saveDocument(crawler, datasetId, userId, embeddingModeId, selector, page, findDocumentId(datasetId, url));
    } catch (Exception e) {
      log.warn("导入网页失败 datasetId={} url={} message={}", datasetId, url, e.getMessage());
      saveFailedDocument(datasetId, userId, url, selector);
    }
  }

  private void syncPage(WebCrawlService crawler, Long datasetId, Long userId, Long embeddingModeId, String selector,
      String sourceUrl, Long documentId) {
    try {
      saveDocument(crawler, datasetId, userId, embeddingModeId, selector, crawler.fetch(sourceUrl, selector), documentId);
    } catch (Exception e) {
      log.warn("同步网页文档失败 datasetId={} documentId={} message={}", datasetId, documentId, e.getMessage());
      Db.update(MARK_DOCUMENT_FAILED, "3", documentId);
    }
  }

  /** 写入或替换一个网页文档，段落与文档在同一个事务里落库。 */
  private int saveDocument(WebCrawlService crawler, Long datasetId, Long userId, Long embeddingModeId, String selector,
      WebCrawlService.Page page, Long documentId) {
    List<WebCrawlService.Paragraph> paragraphs = crawler.split(page.markdown());
    if (paragraphs.isEmpty()) {
      throw new IllegalArgumentException("页面没有可用的正文");
    }
    long id = documentId == null ? SnowflakeIdUtils.id() : documentId;
    KbEmbeddingService embeddingService = Aop.get(KbEmbeddingService.class);
    List<Row> rows = new ArrayList<>();
    int charLength = 0;
    for (WebCrawlService.Paragraph paragraph : paragraphs) {
      charLength += paragraph.content().length();
      PGobject titleVector = paragraph.title().isBlank() ? null
          : embeddingService.getVectorForModel(paragraph.title(), embeddingModeId);
      rows.add(Row.by("id", SnowflakeIdUtils.id()).set("source_id", id).set("source_type", DOCUMENT_TYPE_WEB)
          .set("title", paragraph.title()).set("content", paragraph.content())
          .set("md5", Md5Utils.md5Hex(paragraph.content())).set("status", "2").set("hit_num", 0).set("is_active", true)
          .set("dataset_id", datasetId).set("document_id", id).set("embedding",
              embeddingService.getVectorForModel(paragraph.content(), embeddingModeId))
          .set("title_embedding", titleVector));
    }
    Row document = Row.by("id", id).set("user_id", userId).set("name", page.name()).set("type", DOCUMENT_TYPE_WEB)
        .set("char_length", charLength).set("status", "2").set("is_active", true).set("dataset_id", datasetId)
        .set("paragraph_count", rows.size()).set("hit_handling_method", "optimization")
        .set("directly_return_similarity", 0.9).set("meta", pageMeta(page.url(), selector));
    Db.tx(() -> {
      if (documentId == null) {
        Db.save(MaxKbTableNames.max_kb_document, document);
      } else {
        document.set("update_time", new java.util.Date());
        Db.update(MaxKbTableNames.max_kb_document, document);
        Db.update(DELETE_DOCUMENT_PARAGRAPHS, id);
      }
      Db.batchSave(MaxKbTableNames.max_kb_paragraph, rows, 2000);
      return true;
    });
    return rows.size();
  }

  /** 抓取失败的地址仍然建文档，状态为失败，前端可以看到并重试。 */
  private void saveFailedDocument(Long datasetId, Long userId, String url, String selector) {
    Long existing = findDocumentId(datasetId, url);
    if (existing != null) {
      Db.update(MARK_DOCUMENT_FAILED, "3", existing);
      return;
    }
    Row document = Row.by("id", SnowflakeIdUtils.id()).set("user_id", userId).set("name", url)
        .set("type", DOCUMENT_TYPE_WEB).set("char_length", 0).set("status", "3").set("is_active", true)
        .set("dataset_id", datasetId).set("paragraph_count", 0).set("hit_handling_method", "optimization")
        .set("directly_return_similarity", 0.9).set("meta", pageMeta(url, selector));
    Db.save(MaxKbTableNames.max_kb_document, document);
  }

  /** 同一个地址只对应一个文档，替换同步据此复用已有文档。 */
  private Long findDocumentId(Long datasetId, String sourceUrl) {
    return Db.queryLong(FIND_DOCUMENT_BY_SOURCE_URL, datasetId, sourceUrl);
  }

  /** 整体同步先清空知识库：问题关联、段落、文档按依赖顺序删除。 */
  private void deleteDocuments(Long datasetId) {
    Db.update(DELETE_PROBLEM_PARAGRAPH_MAPPING, datasetId);
    Db.update(DELETE_DATASET_PARAGRAPHS, datasetId);
    Db.update(DELETE_DATASET_DOCUMENTS, datasetId);
  }

  private Row dataset(Long userId, Long datasetId) {
    if (!DatasetAccess.owns(userId, datasetId)) {
      return null;
    }
    return Db.findById(MaxKbTableNames.max_kb_dataset, datasetId);
  }

  /** 创建接口的响应与知识库详情保持一致，document_list 由后台抓取逐步填充。 */
  private Kv createResponse(Long datasetId) {
    Row dataset = Db.findById(MaxKbTableNames.max_kb_dataset, datasetId);
    Kv kv = dataset.toKv();
    kv.set("id", String.valueOf(dataset.getLong("id")));
    kv.set("meta", JsonColumnUtils.toJsonObject(dataset.get("meta")));
    kv.set("document_list", List.of());
    return kv;
  }

  private PGobject pageMeta(String sourceUrl, String selector) {
    return PgObjectUtils.jsonb(JSON.toJSONString(Kv.by("source_url", sourceUrl).set("selector", selector)));
  }

  /** 占用任务槽；返回 false 说明同一个知识库或文档的任务还在执行。 */
  private boolean acquire(String key) {
    return RUNNING.add(key);
  }

  private void release(String key) {
    RUNNING.remove(key);
  }

  /** 提交任务，任务结束（含异常）后释放任务槽。 */
  private void submit(String key, Runnable task) {
    submit(() -> {
      try {
        task.run();
      } finally {
        release(key);
      }
    });
  }

  private void submit(Runnable task) {
    ExecutorServiceUtils.getExecutorService().submit(() -> {
      try {
        task.run();
      } catch (Exception e) {
        log.error("Web 站点任务执行失败", e);
      }
    });
  }

  private static String text(String value) {
    return value == null ? "" : value.strip();
  }
}
