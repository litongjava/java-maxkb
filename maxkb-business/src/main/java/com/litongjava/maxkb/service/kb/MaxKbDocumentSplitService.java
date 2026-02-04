package com.litongjava.maxkb.service.kb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.jfinal.kit.Kv;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.model.result.ResultVo;
import com.litongjava.model.upload.UploadResult;
import com.litongjava.openai.token.OpenAiTokenizer;
import com.litongjava.tio.utils.environment.EnvUtils;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;

/**
 * MaxKbDocumentSplitService
 *
 * 该服务用于将上传的PDF文档拆分为多个Markdown段落，利用OpenAI API将图像转换为文本。
 * 支持多线程并发处理，提高处理效率。
 * 
 * @author 
 * @date 
 */
@Slf4j
public class MaxKbDocumentSplitService {
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
    MaxKbDocumentConvertService maxKbDocumentConvertService = Aop.get(MaxKbDocumentConvertService.class);
    String filename = vo.getName();
    String suffix = "png";
    String apiKey = EnvUtils.getStr("OPENAI_API_KEY");
    String markdown = maxKbDocumentConvertService.toMarkdown(apiKey, data, suffix);
    List<TextSegment> segments = split(markdown);
    // 创建包含文件名和ID的KV对象
    Kv fileSplitResult = Kv.by("name", filename).set("id", vo.getId());
    List<Kv> contents = new ArrayList<>();

    for (TextSegment textSegment : segments) {
      contents.add(Kv.by("title", "").set("content", textSegment.text()));
    }
    fileSplitResult.set("content", contents);
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
