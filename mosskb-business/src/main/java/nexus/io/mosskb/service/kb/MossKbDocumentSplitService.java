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
import nexus.io.model.result.ResultVo;
import nexus.io.model.upload.UploadResult;
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
    String markdown = parsed.text();
    List<TextSegment> segments = split(markdown);
    // 创建包含文件名和ID的KV对象
    Kv fileSplitResult = Kv.by("name", filename).set("id", vo.getId());
    List<Kv> contents = new ArrayList<>();

    for (TextSegment textSegment : segments) {
      contents.add(Kv.by("title", "").set("content", textSegment.text()));
    }
    fileSplitResult.set("content", contents).set("parse_strategy", parsed.strategy()).set("page_count", parsed.pages());
    List<Kv> results = new ArrayList<>();

    results.add(fileSplitResult);

    return ResultVo.ok(results);
  }

  public List<TextSegment> split(String markdown) {
    Document document = new Document(markdown);
    // 使用较大的块大小（2000）和相同的重叠（400）
    DocumentSplitter splitter = DocumentSplitters.recursive(2000, 400, new OpenAiTokenizer());
    List<TextSegment> segments = splitter.split(document);
    return segments;
  }
}
