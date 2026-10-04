package nexus.io.mosskb.service.kb;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.Test;
import static org.junit.Assert.*;
public class DocumentParsingServiceTest {
  private static class Parser extends DocumentParsingService {
    int ocrCalls;
    @Override
    protected String ocr(byte[] data, String filename) {
      ocrCalls++;
      return "Scanned content " + ocrCalls;
    }
    @Override
    protected String ocrPdfPage(String sourceHash, int page, byte[] data) {
      return ocr(data, "page.pdf");
    }
  }

  @Test
  public void pdfUsesTextAndOcrPerPageInOriginalOrder() throws Exception {
    try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      PDPage first = new PDPage();
      pdf.addPage(first);
      try (PDPageContentStream canvas = new PDPageContentStream(pdf, first)) {
        canvas.beginText();
        canvas.setFont(PDType1Font.HELVETICA, 12);
        canvas.newLineAtOffset(40, 700);
        canvas.showText("Native administrative review document with sufficient readable text for extraction.");
        canvas.endText();
      }
      pdf.addPage(new PDPage());
      pdf.save(out);
      Parser parser = new Parser();
      DocumentParsingService.Parsed result = parser.parse(out.toByteArray(), "misleading.txt");
      assertEquals("pdf-mixed", result.strategy());
      assertEquals(2, result.pages());
      assertEquals(1, parser.ocrCalls);
      assertTrue(result.text().indexOf("Native administrative") < result.text().indexOf("Scanned content"));
    }
  }

  @Test
  public void extractsDocxParagraphsAndTableInOrder() throws Exception {
    String xml = "<w:document xmlns:w='urn:test'><w:body><w:p><w:r><w:t>标题</w:t></w:r></w:p><w:tbl><w:tr><w:tc><w:p><w:r><w:t>申请期限</w:t></w:r></w:p></w:tc><w:tc><w:p><w:r><w:t>六十日</w:t></w:r></w:p></w:tc></w:tr></w:tbl></w:body></w:document>";
    Parser parser = new Parser();
    DocumentParsingService.Parsed result = parser.parse(docx(xml), "test.docx");
    assertEquals("docx-structure", result.strategy());
    assertTrue(result.text().contains("|申请期限|六十日|"));
    assertTrue(result.text().indexOf("标题") < result.text().indexOf("申请期限"));
    assertEquals(0, parser.ocrCalls);
  }

  @Test(expected = Exception.class)
  public void rejectsDocxExternalEntities() throws Exception {
    new Parser().parse(docx("<!DOCTYPE x [<!ENTITY secret SYSTEM 'file:///missing'>]><x>&secret;</x>"), "unsafe.docx");
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsEmptyFile() throws Exception {
    new Parser().parse(new byte[0], "empty.txt");
  }

  @Test
  public void decodesLegacyChineseText() throws Exception {
    assertEquals("行政复议申请", new Parser().parse("行政复议申请".getBytes("GB18030"), "law.txt").text());
  }

  @Test
  public void imageUsesOcr() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(20, 20, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", out);
    Parser parser = new Parser();
    assertEquals("image-ocr", parser.parse(out.toByteArray(), "scan.png").strategy());
    assertEquals(1, parser.ocrCalls);
  }

  private byte[] docx(String xml) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(out)) {
      zip.putNextEntry(new ZipEntry("word/document.xml"));
      zip.write(xml.getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
    }
    return out.toByteArray();
  }

  @Test
  public void spreadsheetKeepsSheetNamesAndDisplayedValues() throws Exception {
    try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      org.apache.poi.ss.usermodel.Row row = workbook.createSheet("期限").createRow(0);
      row.createCell(0).setCellValue("申请期限");
      row.createCell(1).setCellValue(60);
      workbook.write(out);
      DocumentParsingService.Parsed result = new Parser().parse(out.toByteArray(), "law.xlsx");
      assertEquals("spreadsheet-structure", result.strategy());
      assertTrue(result.text().contains("## 期限"));
      assertTrue(result.text().contains("|申请期限|60|"));
    }
  }

  @Test
  public void archiveReadsEntriesWithoutExtractingPaths() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(out)) {
      zip.putNextEntry(new ZipEntry("folder/law.txt"));
      zip.write("法律条文".getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
    }
    DocumentParsingService.Parsed result = new Parser().parse(out.toByteArray(), "law.zip");
    assertEquals("zip-documents", result.strategy());
    assertTrue(result.text().contains("folder/law.txt"));
    assertTrue(result.text().contains("法律条文"));
  }

  @Test
  public void repeatedPdfUploadReusesPageCacheDespitePdfSerializationIds() throws Exception {
    class CachedParser extends DocumentParsingService {
      final java.util.Map<String, String> cache = new java.util.HashMap<>();
      final java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
      @Override
      protected String readOcrCache(String key) {
        synchronized (cache) {
          return cache.get(key);
        }
      }
      @Override
      protected void writeOcrCache(String key, String text) {
        synchronized (cache) {
          cache.put(key, text);
        }
      }
      @Override
      protected String recognize(byte[] data, String filename) {
        return "Scanned page content " + calls.incrementAndGet();
      }
    }
    try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      pdf.addPage(new PDPage());
      pdf.addPage(new PDPage());
      pdf.save(out);
      CachedParser parser = new CachedParser();
      DocumentParsingService.Parsed first = parser.parse(out.toByteArray(), "first.pdf");
      int callsAfterFirstUpload = parser.calls.get();
      DocumentParsingService.Parsed second = parser.parse(out.toByteArray(), "renamed.pdf");
      assertEquals(first.text(), second.text());
      // 缓存键是“原文件内容 + 页码”，与序列化单页产生的临时文档 ID 无关：
      // 同一份文件第二次上传必须全部命中缓存，不能再调 OCR。
      assertEquals("重复上传时不应再次调用 OCR", callsAfterFirstUpload, parser.calls.get());
      assertTrue(parser.cache.size() >= 1);
    }
  }

  /** OCR 失败后按退避重试，成功的那一次结果被复用，重试不会作废整份文档。 */
  @Test
  public void retriesOcrOnceAndKeepsTheSuccessfulAttempt() throws Exception {
    class FlakyParser extends DocumentParsingService {
      int calls;
      @Override
      protected String readOcrCache(String key) {
        return null;
      }
      @Override
      protected void writeOcrCache(String key, String text) {
      }
      @Override
      protected String recognize(byte[] data, String filename) throws Exception {
        calls++;
        if (calls == 1) {
          throw new java.io.IOException("OCR服务暂时不可用");
        }
        return "重试后识别到的正文";
      }
    }
    FlakyParser parser = new FlakyParser();
    DocumentParsingService.Parsed result = parser.parse(blankPdf(1), "scan.pdf");
    assertEquals("pdf-ocr", result.strategy());
    assertEquals(0, result.failedPages());
    assertEquals(1, result.ocrPages());
    // 第一次失败，第二次成功。
    assertEquals(2, parser.calls);
  }

  /** 空白扫描页返回空正文属于正常结果，不能让它抛异常作废整份文档。 */
  @Test
  public void blankScannedPageDoesNotFailTheWholeDocument() throws Exception {
    class BlankPageParser extends DocumentParsingService {
      final java.util.List<Integer> pages = new java.util.ArrayList<>();
      @Override
      protected String readOcrCache(String key) {
        return null;
      }
      @Override
      protected void writeOcrCache(String key, String text) {
      }
      @Override
      protected String ocrPdfPage(String sourceHash, int page, byte[] pageData) {
        pages.add(page);
        // 第 2 页是空白页，其余页有正文，模拟扫描件里夹杂的空白页。
        return page == 2 ? "" : "第" + page + "页正文";
      }
    }
    BlankPageParser parser = new BlankPageParser();
    DocumentParsingService.Parsed result = parser.parse(blankPdf(3), "scan.pdf");
    assertEquals("pdf-ocr", result.strategy());
    assertEquals(3, result.pages());
    assertEquals(3, result.ocrPages());
    assertEquals(0, result.failedPages());
    assertTrue(result.text().contains("第1页正文"));
    assertTrue(result.text().contains("第3页正文"));
    assertFalse(result.text().contains("第2页正文"));
    // 正文按物理页顺序输出。
    assertTrue(result.text().indexOf("第1页正文") < result.text().indexOf("第3页正文"));
  }

  /** 每一页 OCR 都失败时给出可重试的错误，而不是“未提取到内容”。 */
  @Test
  public void reportsOcrFailureWhenEveryScannedPageFails() throws Exception {
    class FailingParser extends DocumentParsingService {
      final java.util.Set<Integer> failedPages = new java.util.HashSet<>();
      int calls;
      @Override
      protected String readOcrCache(String key) {
        return null;
      }
      @Override
      protected void writeOcrCache(String key, String text) {
      }
      @Override
      protected String ocrPdfPage(String sourceHash, int page, byte[] pageData) throws Exception {
        calls++;
        failedPages.add(page);
        throw new java.io.IOException("OCR服务未返回有效任务状态");
      }
    }
    FailingParser parser = new FailingParser();
    byte[] pdf = scannedPdf(2);
    try {
      parser.parse(pdf, "scan.pdf");
      fail("每一页 OCR 都失败时应当抛出可重试的异常");
    } catch (java.io.IOException expected) {
      assertTrue(expected.getMessage(), expected.getMessage().contains("OCR"));
    }
    // 两页都试过，并且失败页会重试（页面字节相同的极端情况下重试次数会合并，所以不固定具体次数）。
    assertTrue("第 1 页应当尝试过 OCR：" + parser.failedPages, parser.failedPages.contains(1));
    assertTrue("第 2 页应当尝试过 OCR：" + parser.failedPages, parser.failedPages.contains(2));
    assertTrue("失败页应当重试：" + parser.calls, parser.calls > 2);
  }

  /** 解析进度按页回调，已完成页数与总页数都正确。 */
  @Test
  public void reportsPerPageProgress() throws Exception {
    class CountingParser extends DocumentParsingService {
      final java.util.concurrent.atomic.AtomicInteger pages = new java.util.concurrent.atomic.AtomicInteger();
      @Override
      protected String readOcrCache(String key) {
        return null;
      }
      @Override
      protected void writeOcrCache(String key, String text) {
      }
      @Override
      protected String ocrPdfPage(String sourceHash, int page, byte[] pageData) {
        return "第" + pages.incrementAndGet() + "页正文";
      }
    }
    // 页面并发完成，回调顺序不固定，所以用线程安全的计数容器。
    java.util.concurrent.atomic.AtomicInteger maxCompleted = new java.util.concurrent.atomic.AtomicInteger();
    java.util.concurrent.atomic.AtomicInteger callbackCount = new java.util.concurrent.atomic.AtomicInteger();
    new CountingParser().parse(scannedPdf(3), "scan.pdf", (progress) -> {
      callbackCount.incrementAndGet();
      maxCompleted.accumulateAndGet(progress.completed(), Math::max);
      assertEquals(3, progress.total());
    });
    assertEquals(3, callbackCount.get());
    assertEquals(3, maxCompleted.get());
  }

  /** 生成指定页数的空白 PDF，用来触发逐页 OCR 分支。 */
  private byte[] blankPdf(int pages) throws Exception {
    try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      for (int i = 0; i < pages; i++) {
        pdf.addPage(new PDPage());
      }
      pdf.save(out);
      return out.toByteArray();
    }
  }

  /**
   * 生成每页内容都不相同的低文本 PDF（文本不足 40 个字符，仍会走 OCR）。
   *
   * <p>页与页的字节不同，页面缓存键才不同，才能观察到每页各自的重试次数。
   */
  private byte[] scannedPdf(int pages) throws Exception {
    try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      for (int i = 0; i < pages; i++) {
        PDPage page = new PDPage();
        pdf.addPage(page);
        try (PDPageContentStream canvas = new PDPageContentStream(pdf, page)) {
          canvas.beginText();
          canvas.setFont(PDType1Font.HELVETICA, 12);
          canvas.newLineAtOffset(40, 700);
          canvas.showText("page " + (i + 1));
          canvas.endText();
        }
      }
      pdf.save(out);
      return out.toByteArray();
    }
  }
}
