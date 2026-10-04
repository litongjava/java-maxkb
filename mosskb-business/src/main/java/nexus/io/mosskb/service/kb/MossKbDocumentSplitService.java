package nexus.io.mosskb.service.kb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

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

  /** 把解析结果按分词器切成段落，并组装成前端提交分段需要的结构。 */
  private List<Kv> splitParsed(DocumentParsingService.Parsed parsed, String filename, UploadResult vo) {
    String markdown = parsed.text();
    List<TextSegment> segments = split(markdown);
    Kv fileSplitResult = Kv.by("name", filename).set("id", vo.getId());
    List<Kv> contents = new ArrayList<>();
    for (TextSegment textSegment : segments) {
      contents.add(Kv.by("title", "").set("content", textSegment.text()));
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

  public List<TextSegment> split(String markdown) {
    Document document = new Document(markdown);
    // 使用较大的块大小（2000）和相同的重叠（400）
    DocumentSplitter splitter = DocumentSplitters.recursive(2000, 400, new OpenAiTokenizer());
    List<TextSegment> segments = splitter.split(document);
    return segments;
  }
}
