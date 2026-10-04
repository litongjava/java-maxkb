package nexus.io.mosskb.service.kb;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import lombok.extern.slf4j.Slf4j;
import nexus.io.gitee.*;
import nexus.io.db.activerecord.Db;
import nexus.io.tio.utils.crypto.Md5Utils;

/** Detect the actual document structure before selecting text extraction or OCR. */
@Slf4j
public class DocumentParsingService {

  /** OCR 模型与缓存版本，换模型后旧缓存不会复用。 */
  private static final String OCR_MODEL = GiteeModels.PADDLEOCR_VL_1_5;
  private static final String OCR_CACHE_VERSION = "paddle15";

  /** 单个页面 OCR 的最大尝试次数，用于兜底偶发网络与服务端错误。 */
  private static final int OCR_MAX_ATTEMPTS = 3;

  /** 并发提取的页面数：OCR 是网络调用，页与页之间并发即可，不需要按 CPU 核数拉满。 */
  private static final int OCR_PAGE_PARALLELISM = Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors()));

  /**
   * 同一份内容只提交一次 OCR。
   *
   * <p>逐页并发时相同内容的页可能同时未命中缓存，这里按内容加锁，后到的页等第一次的结果落库后再读缓存，
   * 不重复付费调用，也不会在长进程里堆积历史结果。
   */
  private static final int OCR_LOCKS = 256;
  private static final Object[] OCR_LOCK_STRIPES = new Object[OCR_LOCKS];
  static {
    for (int i = 0; i < OCR_LOCKS; i++) {
      OCR_LOCK_STRIPES[i] = new Object();
    }
  }

  /** 一个页面提取到的内容。 */
  private record PageResult(int page, String text, boolean viaOcr, boolean failed) {}

  /**
   * 解析结果。
   *
   * @param strategy    解析策略
   * @param text        正文
   * @param pages       物理页数，非分页格式为 0
   * @param ocrPages    走 OCR 的页数
   * @param failedPages 尝试多次后仍然失败的页数
   */
  public record Parsed(String strategy, String text, int pages, int ocrPages, int failedPages) {
    /** 兼容只关心策略、正文和页数的调用方。 */
    public Parsed(String strategy, String text, int pages) {
      this(strategy, text, pages, 0, 0);
    }
  }

  public Parsed parse(byte[] data, String filename) throws Exception {
    return parse(data, filename, null);
  }

  /** 解析进度：已完成页数与总页数。 */
  public record Progress(int completed, int total) {
  }

  /**
   * 解析文档。
   *
   * <p>PDF 的每一页独立提取：有文本层直接用文本，扫描页和图片页交给远程 OCR。页面之间没有依赖，
   * 所以并发处理并按页码回填，既保持正文顺序，也缩短大文档的整体耗时。单页 OCR 失败会在重试后记为
   * 空页并继续，不再让一页失败作废整份文档。
   *
   * @param progress 每完成一页回调一次，可为 null
   */
  public Parsed parse(byte[] data, String filename, java.util.function.Consumer<Progress> progress) throws Exception {
    if (data == null || data.length == 0) {
      throw new IllegalArgumentException("文件为空");
    }
    if (data.length > 100 * 1024 * 1024) {
      throw new IllegalArgumentException("文件不能超过100MB");
    }
    String name = filename.toLowerCase(Locale.ROOT);
    Parsed result;
    if (new String(data, 0, Math.min(5, data.length), StandardCharsets.US_ASCII).equals("%PDF-")) {
      result = parsePdf(data, progress);
    } else if (data.length > 4 && data[0] == 'P' && data[1] == 'K' && name.endsWith(".docx")) {
      String extracted = docx(data);
      if (extracted.replaceAll("\\s", "").length() < 40) {
        String images = docxImages(data);
        result = new Parsed(images.isBlank() ? "docx-structure" : "docx-ocr", extracted + images, 0);
      } else {
        result = new Parsed("docx-structure", extracted, 0);
      }
    } else if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
      result = new Parsed("spreadsheet-structure", spreadsheet(data), 0);
    } else if (name.endsWith(".doc")) {
      try (org.apache.poi.hwpf.HWPFDocument word = new org.apache.poi.hwpf.HWPFDocument(new ByteArrayInputStream(data));
          org.apache.poi.hwpf.extractor.WordExtractor extractor = new org.apache.poi.hwpf.extractor.WordExtractor(word)) {
        result = new Parsed("doc-text", extractor.getText(), 0);
      }
    } else if (name.endsWith(".zip")) {
      result = new Parsed("zip-documents", archive(data), 0);
    } else if (name.matches(".*\\.(txt|md|log|csv|json|html|htm)$")) {
      String text;
      try { text = StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(data)).toString(); }
      catch (java.nio.charset.CharacterCodingException e) { text = new String(data, java.nio.charset.Charset.forName("GB18030")); }
      if (name.matches(".*\\.(html|htm)$")) {
        text = org.jsoup.Jsoup.parse(text).wholeText();
      }
      result = new Parsed("text", text, 0);
    } else if (javax.imageio.ImageIO.read(new ByteArrayInputStream(data)) != null) {
      result = new Parsed("image-ocr", ocr(data, filename), 1, 1, 0);
    } else {
      throw new IllegalArgumentException("不支持的文件格式，请转换为PDF、DOCX、TXT或图片后上传");
    }
    if (result.text().isBlank()) {
      // 全部走 OCR 的文档如果每一页都失败，原因是远程服务不可用，不是文档本身没有内容。
      if (result.ocrPages() > 0 && result.ocrPages() == result.failedPages()) {
        throw new IOException("扫描页OCR识别失败，请稍后重试");
      }
      throw new IllegalArgumentException("未提取到内容，请检查文档或使用扫描PDF");
    }
    return result;
  }

  /** 逐页提取 PDF：文本层优先，扫描页走 OCR，页面之间并发处理但按原页序输出。 */
  private Parsed parsePdf(byte[] data, java.util.function.Consumer<Progress> progress) throws Exception {
    try (PDDocument pdf = PDDocument.load(data)) {
      if (pdf.isEncrypted()) {
        throw new IllegalArgumentException("请先解密PDF文件");
      }
      int totalPages = pdf.getNumberOfPages();
      PDFTextStripper stripper = new PDFTextStripper();
      stripper.setSortByPosition(true);
      String sourceHash = Md5Utils.md5Hex(data);
      PageResult[] results = new PageResult[totalPages];
      AtomicInteger completed = new AtomicInteger();
      ForkJoinPool pool = new ForkJoinPool(OCR_PAGE_PARALLELISM);
      try {
        List<ForkJoinTask<?>> tasks = new ArrayList<>(totalPages);
        for (int page = 1; page <= totalPages; page++) {
          int pageNumber = page;
          tasks.add(pool.submit(() -> {
            try {
              results[pageNumber - 1] = extractPdfPage(pdf, stripper, sourceHash, pageNumber);
            } catch (IOException e) {
              throw new UncheckedIOException("解析第 " + pageNumber + " 页失败", e);
            }
            if (progress != null) {
              progress.accept(new Progress(completed.incrementAndGet(), totalPages));
            }
          }));
        }
        for (ForkJoinTask<?> task : tasks) {
          task.join();
        }
      } finally {
        pool.shutdown();
      }
      StringBuilder text = new StringBuilder();
      int ocrPages = 0;
      int nativePages = 0;
      int failedPages = 0;
      List<Integer> failedPageNumbers = new ArrayList<>();
      for (PageResult pageResult : results) {
        if (pageResult.viaOcr()) {
          ocrPages++;
        } else {
          nativePages++;
        }
        if (pageResult.failed()) {
          failedPages++;
          failedPageNumbers.add(pageResult.page());
        }
        if (!pageResult.text().isBlank()) {
          text.append("\n\n> Page ").append(pageResult.page()).append("\n\n").append(pageResult.text());
        }
      }
      if (failedPages > 0) {
        log.warn("PDF {} pages failed OCR after {} attempts: {}", sourceHash, OCR_MAX_ATTEMPTS, failedPageNumbers);
      }
      String suffix = failedPages == 0 ? "" : "-partial";
      String strategy;
      if (ocrPages == 0) {
        strategy = "pdf-text";
      } else if (nativePages == 0) {
        strategy = "pdf-ocr" + suffix;
      } else {
        strategy = "pdf-mixed" + suffix;
      }
      return new Parsed(strategy, text.toString(), totalPages, ocrPages, failedPages);
    }
  }

  /** 提取单页：先尝试文本层，文本不足时把该页单独保存后交给 OCR。 */
  private PageResult extractPdfPage(PDDocument pdf, PDFTextStripper stripper, String sourceHash, int pageNumber)
      throws IOException {
    String page;
    // PDFTextStripper 与 PDDocument 都不是线程安全的，逐页提取时串行访问。
    synchronized (pdf) {
      stripper.setStartPage(pageNumber);
      stripper.setEndPage(pageNumber);
      page = stripper.getText(pdf);
    }
    if (page.replaceAll("\\s", "").length() >= 40 && !page.contains("\uFFFD")) {
      return new PageResult(pageNumber, page, false, false);
    }
    byte[] pageData;
    try (PDDocument single = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      synchronized (pdf) {
        single.importPage(pdf.getPage(pageNumber - 1));
      }
      single.save(out);
      pageData = out.toByteArray();
    }
    // OCR 结果为空既可能是空白页，也可能是服务没能识别，两种都按空页处理并保证页码占位。
    // 缓存键固定用“原文件内容 + 页码”：序列化单页时 PDFBox 会写入新的内部文档 ID，
    // 同一页重复序列化的字节并不相同，用这些字节做键会让缓存永远不命中。
    boolean[] failed = new boolean[1];
    String[] recognized = new String[1];
    boolean ok = withOcrRetry(() -> ocrPdfPage(sourceHash, pageNumber, pageData), recognized, failed,
        sourceHash + ":page:" + pageNumber + ":" + OCR_CACHE_VERSION);
    return new PageResult(pageNumber, ok ? recognized[0] : "", true, failed[0]);
  }

  /**
   * OCR 重试：失败时按 1s、2s 退避重试，最后一次仍失败就记为失败页。
   *
   * <p>“未识别到内容”是正常返回，不是异常，因此不在这里重试。
   *
   * @return 是否拿到结果
   */
  private boolean withOcrRetry(OcrCall call, String[] result, boolean[] failed, String describe) {
    for (int attempt = 1; attempt <= OCR_MAX_ATTEMPTS; attempt++) {
      try {
        result[0] = call.recognize();
        return true;
      } catch (Exception e) {
        log.warn("OCR attempt {}/{} failed for {}: {}", attempt, OCR_MAX_ATTEMPTS, describe, e.getMessage());
        if (attempt < OCR_MAX_ATTEMPTS) {
          try {
            Thread.sleep(1000L * attempt);
          } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            failed[0] = true;
            return false;
          }
        }
      }
    }
    log.warn("OCR gave up for {}", describe);
    failed[0] = true;
    return false;
  }

  /** 一次 OCR 调用，用于把重试逻辑包在可覆盖的 OCR 方法外面。 */
  @FunctionalInterface
  private interface OcrCall {
    String recognize() throws Exception;
  }

  private String docx(byte[] data) throws Exception {
    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(data))) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        if (!entry.getName().equals("word/document.xml")) {
          continue;
        }
        byte[] xml = zip.readNBytes(20 * 1024 * 1024 + 1);
        if (xml.length > 20 * 1024 * 1024) {
          throw new IllegalArgumentException("DOCX内容过大");
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        StringBuilder out = new StringBuilder();
        Node body = doc.getElementsByTagNameNS("*", "body").item(0);
        for (Node node = body.getFirstChild(); node != null; node = node.getNextSibling()) {
          if ("p".equals(node.getLocalName())) {
            out.append(text(node)).append("\n\n");
          }
          if ("tbl".equals(node.getLocalName())) {
            NodeList rows = ((Element) node).getElementsByTagNameNS("*", "tr");
            for (int r = 0; r < rows.getLength(); r++) {
              NodeList cells = ((Element) rows.item(r)).getElementsByTagNameNS("*", "tc");
              out.append('|');
              for (int c = 0; c < cells.getLength(); c++) out.append(text(cells.item(c)).replace("|", "\\|")).append('|');
              out.append('\n');
              if (r == 0) { out.append('|'); for (int c = 0; c < cells.getLength(); c++) out.append("---|"); out.append('\n'); }
            }
            out.append('\n');
          }
        }
        return out.toString();
      }
    }
    throw new IllegalArgumentException("DOCX缺少正文");
  }
  private String text(Node node) {
    StringBuilder out = new StringBuilder();
    NodeList texts = ((Element) node).getElementsByTagNameNS("*", "t");
    for (int i = 0; i < texts.getLength(); i++) out.append(texts.item(i).getTextContent());
    return out.toString();
  }

  private String spreadsheet(byte[] data) throws Exception {
    try (org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(new ByteArrayInputStream(data))) {
      org.apache.poi.ss.usermodel.DataFormatter formatter = new org.apache.poi.ss.usermodel.DataFormatter(Locale.ROOT);
      formatter.setUseCachedValuesForFormulaCells(true);
      StringBuilder out = new StringBuilder();
      int rows = 0;
      for (org.apache.poi.ss.usermodel.Sheet sheet : workbook) {
        out.append("\n## ").append(sheet.getSheetName()).append("\n\n");
        for (org.apache.poi.ss.usermodel.Row row : sheet) {
          if (++rows > 100000 || row.getLastCellNum() > 500 || out.length() > 20 * 1024 * 1024) {
            throw new IllegalArgumentException("表格内容过大，请拆分后上传");
          }
          for (int i = 0; i < row.getLastCellNum(); i++) {
            out.append('|').append(formatter.formatCellValue(row.getCell(i)).replace("|", "\\|").replace('\n', ' '));
          }
          out.append("|\n");
        }
      }
      return out.toString();
    }
  }

  private String docxImages(byte[] data) throws Exception {
    StringBuilder out = new StringBuilder();
    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(data))) {
      ZipEntry entry;
      int count = 0;
      while ((entry = zip.getNextEntry()) != null) {
        if (!entry.getName().startsWith("word/media/") || entry.isDirectory()) {
          continue;
        }
        byte[] bytes = zip.readNBytes(20 * 1024 * 1024 + 1);
        if (++count > 50 || bytes.length > 20 * 1024 * 1024) {
          throw new IllegalArgumentException("DOCX内嵌图片过多或过大");
        }
        if (javax.imageio.ImageIO.read(new ByteArrayInputStream(bytes)) != null) {
          out.append("\n\n").append(ocr(bytes, entry.getName().substring("word/media/".length())));
        }
      }
    }
    return out.toString();
  }

  private String archive(byte[] data) throws Exception {
    StringBuilder out = new StringBuilder();
    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(data))) {
      ZipEntry entry;
      int count = 0;
      long total = 0;
      while ((entry = zip.getNextEntry()) != null) {
        if (entry.isDirectory()) {
          continue;
        }
        String name = entry.getName().replace('\\', '/');
        if (name.startsWith("__MACOSX/") || name.endsWith(".DS_Store")) {
          continue;
        }
        if (name.toLowerCase(Locale.ROOT).endsWith(".zip") || ++count > 50) {
          throw new IllegalArgumentException("ZIP最多包含50个文件，不支持嵌套ZIP");
        }
        byte[] bytes = zip.readNBytes(100 * 1024 * 1024 + 1);
        total += bytes.length;
        if (total > 100 * 1024 * 1024) {
          throw new IllegalArgumentException("ZIP解压后的内容不能超过100MB");
        }
        out.append("\n\n# ").append(name).append("\n\n").append(parse(bytes, name).text());
      }
    }
    return out.toString();
  }
  protected String ocr(byte[] data, String filename) throws Exception {
    return ocrWithCache(data, filename, Md5Utils.md5Hex(data) + ":" + OCR_CACHE_VERSION);
  }

  protected String ocrPdfPage(String sourceHash, int page, byte[] pageData) throws Exception {
    // Serializing a PDF page can generate a fresh internal document ID. Cache by
    // original file content and page number instead of the transient PDF bytes.
    return ocrWithCache(pageData, "page.pdf", sourceHash + ":page:" + page + ":" + OCR_CACHE_VERSION);
  }

  private String ocrWithCache(byte[] data, String filename, String hash) throws Exception {
    String cached = readOcrCache(hash);
    if (cached != null && !cached.isBlank()) {
      return textOnly(cached);
    }
    synchronized (OCR_LOCK_STRIPES[Math.floorMod(hash.hashCode(), OCR_LOCKS)]) {
      // 同一份内容的第一个请求可能已经写好缓存，这里再查一次就能省掉重复的付费调用。
      cached = readOcrCache(hash);
      if (cached != null && !cached.isBlank()) {
        return textOnly(cached);
      }
      String markdown = recognize(data, filename);
      if (markdown != null && !markdown.isBlank()) {
        writeOcrCache(hash, markdown);
      }
      return markdown;
    }
  }

  protected String readOcrCache(String hash) {
    return Db.queryStr("select content from moss_kb_document_markdown_cache where id=?", hash);
  }

  protected void writeOcrCache(String hash, String markdown) {
    Db.update("insert into moss_kb_document_markdown_cache(id,content) values(?,?) on conflict(id) do update set content=excluded.content", hash, markdown);
  }

  protected String recognize(byte[] data, String filename) throws Exception {
    GiteeClient client = new GiteeClient();
    GiteeDocumentParseRequest request = new GiteeDocumentParseRequest();
    request.setModel(OCR_MODEL);
    request.setPrompt(GiteePromptConst.pdf_to_markdown_prompt);
    request.setInclude_image(false);
    request.setInclude_image_base64(false);
    GiteeTaskResponse task = client.parseDocument(data, filename, request);
    long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.MINUTES.toNanos(8);
    while (!Arrays.asList("success", "succeeded", "completed").contains(String.valueOf(task.getStatus()).toLowerCase(Locale.ROOT))) {
      if (task.getStatus() == null || task.getTask_id() == null) {
        throw new IOException("OCR服务未返回有效任务状态");
      }
      if (Arrays.asList("failed", "failure", "cancelled", "canceled").contains(task.getStatus().toLowerCase(Locale.ROOT))) {
        throw new IOException("OCR任务失败: " + task.getTask_id());
      }
      if (System.nanoTime() > deadline) {
        throw new IOException("OCR任务超时: " + task.getTask_id());
      }
      Thread.sleep(2000); task = client.getTask(task.getTask_id());
    }
    // 扫描页可能是空白页或整页图片，服务会正常返回空正文，这里按空内容处理，由调用方决定是否记录。
    String markdown = textOnly(GiteeSimpleMarkdownUtils.toMarkdown(task.getOutput(), null));
    if (markdown == null || markdown.isBlank()) {
      log.info("OCR returned no text for {}", filename);
      return "";
    }
    return markdown;
  }

  private String textOnly(String markdown) {
    if (markdown == null) {
      return "";
    }
    return markdown.replaceAll("(?is)<img\\b[^>]*>", "")
        .replaceAll("(?is)<div[^>]*>\\s*</div>", "").replaceAll("!\\[[^\\]]*\\]\\([^)]*\\)", "");
  }
}
