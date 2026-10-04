package nexus.io.mosskb.service.kb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jfinal.kit.Kv;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;
import nexus.io.jfinal.aop.Aop;
import nexus.io.model.result.ResultVo;
import nexus.io.model.upload.UploadResult;
import nexus.io.mosskb.utils.ExecutorServiceUtils;
import nexus.io.openai.token.OpenAiTokenizer;

/**
 * MossKbDocumentSplitService
 *
 * 检测文档内容，选择结构化提取或远程 OCR 后分段。
 * 
 * @author 
 * @date 
 */
@Slf4j
public class MossKbDocumentSplitService {

  /** Markdown 标题行，用于给分段补上所属标题。 */
  private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.*)$");

  /** 单个分段标题的长度上限，和段落标题列的容量保持一致。 */
  private static final int TITLE_MAX_LENGTH = 255;

  /**
   * 拆分文档（多并发）
   *
   * @param data 文档二进制数据
   * @param vo 上传结果对象
   * @return 拆分后的结果
   * @throws IOException 可能的IO异常
   * @throws InterruptedException 线程中断异常
   * @throws ExecutionException 执行异常
   */
  public ResultVo split(byte[] data, UploadResult vo) throws IOException, InterruptedException, ExecutionException {
    String filename = vo.getName();
    DocumentParsingService.Parsed parsed;
    try { parsed = new DocumentParsingService().parse(data, filename); } catch (Exception e) { throw new IOException(e.getMessage(), e); }
    return ResultVo.ok(splitParsed(parsed, filename, vo));
  }

  /**
   * 异步拆分：立刻返回 task_id，解析与分段交给后台虚拟线程，前端轮询任务状态取结果。
   *
   * <p>大文档的解析要几分钟，占着请求线程等待既容易超时，也让前端只能干等。
   */
  public Long splitAsync(byte[] data, UploadResult vo, Long userId) {
    MossKbDocumentSplitTaskService taskService = Aop.get(MossKbDocumentSplitTaskService.class);
    Long taskId = taskService.start(userId, vo, data == null ? 0L : (long) data.length);
    ExecutorServiceUtils.getDocumentParseExecutor().execute(() -> runSplit(data, vo, taskId, taskService));
    return taskId;
  }

  /** 后台解析入口：把解析进度与最终分段写回任务表，异常统一落到任务失败原因。 */
  private void runSplit(byte[] data, UploadResult vo, Long taskId, MossKbDocumentSplitTaskService taskService) {
    String filename = vo.getName();
    try {
      DocumentParsingService.Parsed parsed = new DocumentParsingService().parse(data, filename,
          (progress) -> taskService.markProgress(taskId, progress.completed(), progress.total()));
      taskService.markTotal(taskId, parsed.pages());
      taskService.markProgress(taskId, parsed.pages(), parsed.pages());
      taskService.complete(taskId, splitParsed(parsed, filename, vo));
    } catch (Exception e) {
      taskService.fail(taskId, e.getMessage());
    }
  }

  /** 把解析结果按分词器切成段落，并组装成前端提交分段需要的结构；包内可见便于直接回归测试。 */
  List<Kv> splitParsed(DocumentParsingService.Parsed parsed, String filename, UploadResult vo) {
    String markdown = parsed.text();
    List<TextSegment> segments = split(markdown);
    Kv fileSplitResult = Kv.by("name", filename).set("id", vo.getId());
    List<String> titles = segmentTitles(segments, filename);
    List<Kv> contents = new ArrayList<>();
    for (int i = 0; i < segments.size(); i++) {
      contents.add(Kv.by("title", titles.get(i)).set("content", segments.get(i).text()));
    }
    fileSplitResult.set("content", contents).set("parse_strategy", parsed.strategy()).set("page_count", parsed.pages());
    if (parsed.ocrPages() > 0) {
      fileSplitResult.set("ocr_page_count", parsed.ocrPages());
    }
    if (parsed.failedPages() > 0) {
      fileSplitResult.set("failed_page_count", parsed.failedPages());
    }
    List<Kv> results = new ArrayList<>();
    results.add(fileSplitResult);
    return results;
  }

  /**
   * 为每个分段补上所属的标题。
   *
   * <p>解析结果本身就是 Markdown，标题层级构成标题链，和网页导入的 {@code WebCrawlService#split} 保持一致。
   * 分段里出现过标题时用第一个标题的标题链：一个分段常常横跨几页和小节，用第一个标题能保住文档标题，
   * 用最后一个就会只剩最后一个小节。整篇没有标题（例如纯文本或识别不出小标题的扫描件）时退回原文件名，
   * 这样分段列表不会出现一整列空标题。
   */
  List<String> segmentTitles(List<TextSegment> segments, String filename) {
    List<String> titles = new ArrayList<>(segments.size());
    List<String> chain = new ArrayList<>();
    for (TextSegment segment : segments) {
      List<String> firstTitle = new ArrayList<>();
      fillChain(chain, segment.text(), firstTitle);
      titles.add(buildTitle(firstTitle.isEmpty() ? chain : firstTitle, filename));
    }
    return titles;
  }

  /** 按当前标题链生成标题：没有标题链时用原文件名兜底，并按段落标题列的长度截断。 */
  private String buildTitle(List<String> chain, String filename) {
    String title = chain.isEmpty() ? filename : String.join(" ", chain);
    title = title.strip();
    if (title.length() <= TITLE_MAX_LENGTH) {
      return title;
    }
    return title.substring(0, TITLE_MAX_LENGTH);
  }

  /**
   * 扫描分段里的 Markdown 标题并按层级维护标题链：低级标题替换掉同级的旧标题，一级标题开始新的顶层章节。
   *
   * @param firstTitle 非 null 时记下本段第一个标题的标题链，供分段标题使用
   */
  private void fillChain(List<String> chain, String text, List<String> firstTitle) {
    if (text == null || text.isEmpty()) {
      return;
    }
    for (String line : text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1)) {
      Matcher matcher = HEADING.matcher(line.strip());
      if (!matcher.matches()) {
        continue;
      }
      int level = matcher.group(1).length();
      String heading = matcher.group(2).strip();
      if (heading.isEmpty()) {
        continue;
      }
      if (level == 1 && !chain.isEmpty()) {
        chain.clear();
      }
      while (chain.size() >= level) {
        chain.remove(chain.size() - 1);
      }
      if (chain.isEmpty() || !chain.get(chain.size() - 1).equals(heading)) {
        chain.add(heading);
      }
      if (firstTitle != null && firstTitle.isEmpty()) {
        firstTitle.addAll(chain);
      }
    }
  }

  public List<TextSegment> split(String markdown) {
    Document document = new Document(markdown);
    // 使用较大的块大小（2000）和相同的重叠（400）
    DocumentSplitter splitter = DocumentSplitters.recursive(2000, 400, new OpenAiTokenizer());
    List<TextSegment> segments = splitter.split(document);
    return segments;
  }
}
