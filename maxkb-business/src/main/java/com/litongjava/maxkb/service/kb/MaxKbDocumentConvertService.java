package com.litongjava.maxkb.service.kb;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import com.google.common.util.concurrent.Striped;
import com.jfinal.template.Engine;
import com.litongjava.db.TableInput;
import com.litongjava.db.TableResult;
import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.maxkb.constant.MaxKbTableNames;
import com.litongjava.maxkb.utils.ExecutorServiceUtils;
import com.litongjava.openai.chat.ChatResponseUsage;
import com.litongjava.openai.chat.OpenAiChatResponse;
import com.litongjava.openai.client.OpenAiClient;
import com.litongjava.openai.consts.OpenAiModels;
import com.litongjava.table.services.ApiTable;
import com.litongjava.tio.utils.crypto.Md5Utils;
import com.litongjava.tio.utils.hutool.FileUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MaxKbDocumentConvertService {
  Striped<Lock> locks = Striped.lock(1024);
  private static final int MAX_HEIGHT = 2200;

  /**
   * 将文档转换为 Markdown 格式（多并发）
   *
   * @param apiKey OpenAI API 密钥
   * @param data 文档二进制数据
   * @param suffix 文件后缀
   * @return Markdown 文本
   * @throws IOException 可能的IO异常
   * @throws InterruptedException 线程中断异常
   * @throws ExecutionException 执行异常
   */
  public String toMarkdown(String apiKey, byte[] data, String suffix) throws IOException, InterruptedException, ExecutionException {
    String md5 = Md5Utils.md5Hex(data);
    log.info("Processing document with MD5: {}", md5);

    // 查询缓存以检查是否已经处理过该文档
    TableInput ti = TableInput.create().columns("target,content").set("id", md5);
    TableResult<Row> tableResult = ApiTable.get(MaxKbTableNames.max_kb_document_markdown_cache, ti);
    Row record = tableResult.getData();
    boolean exists = false;
    String target = null;

    if (record != null) {
      target = record.getStr("target");
      if (target != null) {
        exists = true;
        File file = new File(target);
        if (file.exists()) {
          log.info("Markdown found in cache at {}", target);
          return FileUtil.readString(file);
        }
      }
      String content = record.getStr("content");
      if (content != null) {
        log.info("Markdown content found in cache");
        return content;
      }
    }

    // 将PDF每一页转换为图像字节数组，并处理可能过大的图像
    List<byte[]> documentBytes = new ArrayList<>();
    int totalPages = 0;
    try (PDDocument document = PDDocument.load(new ByteArrayInputStream(data))) {
      totalPages = document.getNumberOfPages();
      PDFRenderer renderer = new PDFRenderer(document);
      for (int i = 0; i < totalPages; i++) {
        BufferedImage bufferedImage = renderer.renderImageWithDPI(i, 144);
        // 分割图像以处理高度超过MAX_HEIGHT的情况
        List<byte[]> splitImages = toBytes(bufferedImage, suffix);
        documentBytes.addAll(splitImages);
      }
    }

    // 使用CompletionService来管理并发任务
    CompletionService<String> completionService = new ExecutorCompletionService<>(ExecutorServiceUtils.getExecutorService());
    List<Future<String>> futures = new ArrayList<>();
    for (byte[] imageBytes : documentBytes) {
      futures.add(completionService.submit(() -> convertPdfPageToMarkdown(apiKey, imageBytes, suffix)));
    }

    // 等待所有任务完成并按提交顺序存储结果
    List<String> markdowns = new ArrayList<>(Collections.nCopies(documentBytes.size(), null));
    for (int i = 0; i < futures.size(); i++) {
      Future<String> future = completionService.take();
      int pageIndex = futures.indexOf(future); // 获取任务对应的索引
      markdowns.set(pageIndex, future.get()); // 按索引顺序存储结果
    }

    // 组合所有Markdown内容
    StringBuilder combinedMarkdown = new StringBuilder();
    for (String markdown : markdowns) {
      combinedMarkdown.append(markdown);
    }

    // 保存Markdown到文件
    target = "markdowns/" + md5 + ".md";
    new File(target).getParentFile().mkdirs();
    FileUtil.writeString(combinedMarkdown.toString(), target, "UTF-8");
    log.info("Markdown saved to {}", target);

    // 更新或保存缓存记录
    if (exists) {
      Db.update(MaxKbTableNames.max_kb_document_markdown_cache, Row.by("id", md5).set("target", target).set("content", combinedMarkdown));
      log.info("Cache updated for document MD5: {}", md5);
    } else {
      Db.save(MaxKbTableNames.max_kb_document_markdown_cache, Row.by("id", md5).set("target", target).set("content", combinedMarkdown));
      log.info("Cache saved for new document MD5: {}", md5);
    }

    return combinedMarkdown.toString();
  }

  /**
   * 将 PDF 页面转换为 Markdown（多并发）
   *
   * @param apiKey OpenAI API 密钥
   * @param imageBytes 图片字节数组
   * @param suffix 文件后缀
   * @return Markdown 文本
   * @throws IOException 可能的IO异常
   */
  public String convertPdfPageToMarkdown(String apiKey, byte[] imageBytes, String suffix) throws IOException {
    String id = Md5Utils.md5Hex(imageBytes);
    String sql = String.format("SELECT content FROM %s WHERE id=?", MaxKbTableNames.max_kb_document_markdown_page_cache);

    // 查询缓存以避免重复处理
    String content = Db.queryStr(sql, id);
    if (content != null) {
      log.debug("Content found in page cache for ID: {}", id);
      return content;
    }

    // 保存图像文件
    String imageName = id + "." + suffix;
    String imagePath = "images/" + imageName;
    File imageFile = new File(imagePath);
    imageFile.getParentFile().mkdirs();
    FileUtil.writeBytes(imageBytes, imageFile);
    log.debug("Image saved to {}", imagePath);

    // 调用OpenAI API将图像转换为文本
    long start = System.currentTimeMillis();
    OpenAiChatResponse chatResponseVo = null;
    String imageToTextPrompt = Engine.use().getTemplate("image_to_text_prompt.txt").renderToString();
    try {
      chatResponseVo = OpenAiClient.chatWithImage(apiKey, imageToTextPrompt, imageBytes, suffix);
    } catch (Exception e) {
      try {
        chatResponseVo = OpenAiClient.chatWithImage(apiKey, imageToTextPrompt, imageBytes, suffix);
      } catch (Exception e1) {
        chatResponseVo = OpenAiClient.chatWithImage(apiKey, imageToTextPrompt, imageBytes, suffix);
      }
    }

    content = chatResponseVo.getChoices().get(0).getMessage().getContent();
    if (content.startsWith("```markdown")) {
      content = content.substring(11, content.length() - 3);
    }

    ChatResponseUsage usage = chatResponseVo.getUsage();
    TableInput saveInput = TableInput.by("id", id).set("target", imagePath).set("content", content)
        .set("elapsed", System.currentTimeMillis() - start).set("model", OpenAiModels.GPT_4O)
        .set("system_fingerprint", chatResponseVo.getSystem_fingerprint()).set("completion_tokens", usage.getCompletion_tokens())
        .set("prompt_tokens", usage.getPrompt_tokens()).set("total_tokens", usage.getTotal_tokens());

    // 再次检查缓存以防止并发情况下的重复保存
    String cacheContent = Db.queryStr(sql, id);
    if (cacheContent != null) {
      log.debug("Content found in page cache during save for ID: {}", id);
      return cacheContent;
    }

    // 同步保存操作，避免多线程同时写入
    Lock lock = locks.get(id);
    lock.lock();
    try {
      // 再次检查以确保线程安全
      cacheContent = Db.queryStr(sql, id);
      if (cacheContent == null) {
        ApiTable.save(MaxKbTableNames.max_kb_document_markdown_page_cache, saveInput);
        log.debug("Content cached for page ID: {}", id);
        return content;
      } else {
        return cacheContent;
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * 将 BufferedImage 转换为字节数组，如果图像高度超过MAX_HEIGHT，则进行垂直分割
   *
   * @param bufferedImage 图片对象
   * @param suffix 文件后缀
   * @return 字节数组列表
   * @throws IOException 可能的IO异常
   */
  private List<byte[]> toBytes(BufferedImage bufferedImage, String suffix) throws IOException {
    List<byte[]> imageBytesList = new ArrayList<>();
    if (bufferedImage.getHeight() <= MAX_HEIGHT) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(bufferedImage, suffix, baos);
      imageBytesList.add(baos.toByteArray());
    } else {
      int numParts = (int) Math.ceil((double) bufferedImage.getHeight() / MAX_HEIGHT);
      for (int i = 0; i < numParts; i++) {
        int y = i * MAX_HEIGHT;
        int height = Math.min(MAX_HEIGHT, bufferedImage.getHeight() - y);
        BufferedImage subImage = bufferedImage.getSubimage(0, y, bufferedImage.getWidth(), height);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(subImage, suffix, baos);
        imageBytesList.add(baos.toByteArray());
        log.debug("Image split into part {}/{}", i + 1, numParts);
      }
    }
    return imageBytesList;
  }
}