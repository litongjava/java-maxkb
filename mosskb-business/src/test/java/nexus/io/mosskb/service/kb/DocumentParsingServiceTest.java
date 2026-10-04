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
      int calls;
      @Override
      protected String readOcrCache(String key) {
        return cache.get(key);
      }
      @Override
      protected void writeOcrCache(String key, String text) {
        cache.put(key, text);
      }
      @Override
      protected String recognize(byte[] data, String filename) {
        calls++;
        return "Scanned page content " + calls;
      }
    }
    try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      pdf.addPage(new PDPage());
      pdf.addPage(new PDPage());
      pdf.save(out);
      CachedParser parser = new CachedParser();
      DocumentParsingService.Parsed first = parser.parse(out.toByteArray(), "first.pdf");
      DocumentParsingService.Parsed second = parser.parse(out.toByteArray(), "renamed.pdf");
      assertEquals(first.text(), second.text());
      assertEquals(2, parser.calls);
      assertEquals(2, parser.cache.size());
    }
  }
}
