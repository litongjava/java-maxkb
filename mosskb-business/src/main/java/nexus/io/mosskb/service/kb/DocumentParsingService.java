package nexus.io.mosskb.service.kb;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import nexus.io.gitee.*;
import nexus.io.db.activerecord.Db;
import nexus.io.tio.utils.crypto.Md5Utils;

/** Detect the actual document structure before selecting text extraction or OCR. */
public class DocumentParsingService {
  public record Parsed(String strategy, String text, int pages) {}
  public Parsed parse(byte[] data, String filename) throws Exception {
    if (data == null || data.length == 0) {
      throw new IllegalArgumentException("文件为空");
    }
    if (data.length > 100 * 1024 * 1024) {
      throw new IllegalArgumentException("文件不能超过100MB");
    }
    String name = filename.toLowerCase(Locale.ROOT);
    Parsed result;
    if (new String(data, 0, Math.min(5, data.length), StandardCharsets.US_ASCII).equals("%PDF-")) {
      try (PDDocument pdf = PDDocument.load(data)) {
        if (pdf.isEncrypted()) {
          throw new IllegalArgumentException("请先解密PDF文件");
        }
        PDFTextStripper stripper = new PDFTextStripper(); stripper.setSortByPosition(true);
        String sourceHash = Md5Utils.md5Hex(data);
        StringBuilder text = new StringBuilder(); int nativePages = 0;
        for (int i = 1; i <= pdf.getNumberOfPages(); i++) {
          stripper.setStartPage(i); stripper.setEndPage(i);
          String page = stripper.getText(pdf);
          if (page.replaceAll("\\s", "").length() >= 40 && !page.contains("\uFFFD")) {
            text.append("\n\n> Page ").append(i).append("\n\n").append(page); nativePages++;
          } else {
            try (PDDocument single = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
              single.importPage(pdf.getPage(i - 1)); single.save(out);
              text.append("\n\n> Page ").append(i).append("\n\n").append(ocrPdfPage(sourceHash, i, out.toByteArray()));
            }
          }
        }
        result = new Parsed(nativePages == pdf.getNumberOfPages() ? "pdf-text" : nativePages == 0 ? "pdf-ocr" : "pdf-mixed", text.toString(), pdf.getNumberOfPages());
      }
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
      result = new Parsed("image-ocr", ocr(data, filename), 1);
    } else {
      throw new IllegalArgumentException("不支持的文件格式，请转换为PDF、DOCX、TXT或图片后上传");
    }
    if (result.text().isBlank()) {
      throw new IllegalArgumentException("未提取到内容，请检查文档或使用扫描PDF");
    }
    return result;
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
    return ocrWithCache(data, filename, Md5Utils.md5Hex(data) + ":paddle15");
  }

  protected String ocrPdfPage(String sourceHash, int page, byte[] pageData) throws Exception {
    // Serializing a PDF page can generate a fresh internal document ID. Cache by
    // original file content and page number instead of the transient PDF bytes.
    return ocrWithCache(pageData, "page.pdf", sourceHash + ":page:" + page + ":paddle15");
  }

  private String ocrWithCache(byte[] data, String filename, String hash) throws Exception {
    String cached = readOcrCache(hash);
    if (cached != null && !cached.isBlank()) {
      return textOnly(cached);
    }
    String markdown = recognize(data, filename);
    writeOcrCache(hash, markdown);
    return markdown;
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
    request.setModel("PaddleOCR-VL-1.5");
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
    String markdown = textOnly(GiteeSimpleMarkdownUtils.toMarkdown(task.getOutput(), null));
    if (markdown == null || markdown.isBlank()) {
      throw new IOException("OCR未返回正文");
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
