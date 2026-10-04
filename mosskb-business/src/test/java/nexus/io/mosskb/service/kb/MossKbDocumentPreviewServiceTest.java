package nexus.io.mosskb.service.kb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Test;

public class MossKbDocumentPreviewServiceTest {

  @Test
  public void docxPreviewKeepsParagraphsAndTablesInOrder() throws Exception {
    String xml = "<w:document xmlns:w='urn:test'><w:body>"
        + "<w:p><w:r><w:t>第七十七条</w:t></w:r></w:p>"
        + "<w:tbl><w:tr><w:tc><w:p><w:r><w:t>申请期限</w:t></w:r></w:p></w:tc>"
        + "<w:tc><w:p><w:r><w:t>六十日</w:t></w:r></w:p></w:tc></w:tr></w:tbl>"
        + "</w:body></w:document>";
    String html = new MossKbDocumentPreviewService().docxToHtml(docx(xml));
    assertTrue(html.contains("<p>第七十七条</p>"));
    assertTrue(html.contains("<table class=\"preview-table\">"));
    assertTrue(html.contains("<td>申请期限</td>"));
    assertTrue(html.indexOf("第七十七条") < html.indexOf("申请期限"));
  }

  /** 文档正文里的尖括号必须转义，否则预览会把文档内容当成标签执行。 */
  @Test
  public void docxPreviewEscapesDocumentText() throws Exception {
    String xml = "<w:document xmlns:w='urn:test'><w:body>"
        + "<w:p><w:r><w:t>&lt;script&gt;alert(1)&lt;/script&gt;</w:t></w:r></w:p>"
        + "</w:body></w:document>";
    String html = new MossKbDocumentPreviewService().docxToHtml(docx(xml));
    assertTrue(html.contains("&lt;script&gt;"));
    assertFalse(html.contains("<script>"));
  }

  @Test
  public void spreadsheetPreviewKeepsSheetNameAndCellText() throws Exception {
    try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      org.apache.poi.ss.usermodel.Row row = workbook.createSheet("复议期限").createRow(0);
      row.createCell(0).setCellValue("申请期限");
      row.createCell(1).setCellValue(60);
      workbook.write(out);
      MossKbDocumentPreviewService.Preview preview = new MossKbDocumentPreviewService()
          .spreadsheetToHtml(out.toByteArray());
      assertEquals(MossKbDocumentPreviewService.KIND_HTML, preview.kind());
      assertTrue(preview.content().contains("<h4>复议期限</h4>"));
      assertTrue(preview.content().contains("<td>申请期限</td><td>60</td>"));
      assertFalse(preview.truncated());
    }
  }

  @Test
  public void csvPreviewHandlesQuotedCells() {
    MossKbDocumentPreviewService.Preview preview = new MossKbDocumentPreviewService()
        .csvToHtml("名称,说明\n\"行政复议,申请\",\"含\"\"引号\"\"\"\n");
    assertTrue(preview.content().contains("<td>行政复议,申请</td>"));
    // 单元格里的双引号属于正文，输出 HTML 时按实体转义。
    assertTrue(preview.content().contains("<td>含&quot;引号&quot;</td>"));
  }

  @Test
  public void csvLineSplitsOnUnquotedCommasOnly() {
    List<String> cells = MossKbDocumentPreviewService.splitCsvLine("a,\"b,c\",d");
    assertEquals(3, cells.size());
    assertEquals("a", cells.get(0));
    assertEquals("b,c", cells.get(1));
    assertEquals("d", cells.get(2));
  }

  @Test
  public void textFilesFallBackToGb18030() {
    assertEquals("行政复议申请", MossKbDocumentPreviewService.readText("行政复议申请".getBytes(java.nio.charset.Charset.forName("GB18030"))));
    assertEquals("行政复议申请", MossKbDocumentPreviewService.readText("行政复议申请".getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  public void zipPreviewListsEntries() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(out)) {
      zip.putNextEntry(new ZipEntry("folder/law.txt"));
      zip.write("法律条文".getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
    }
    assertTrue(new MossKbDocumentPreviewService().zipListing(out.toByteArray()).contains("folder/law.txt"));
  }

  @Test
  public void contentTypeCoversPreviewableFiles() {
    assertEquals("application/pdf", MossKbDocumentPreviewService.contentType("pdf"));
    assertEquals("image/png", MossKbDocumentPreviewService.contentType("png"));
    assertEquals("image/webp", MossKbDocumentPreviewService.contentType("webp"));
    assertEquals("application/octet-stream", MossKbDocumentPreviewService.contentType("zip"));
  }

  @Test
  public void dispositionKeepsChineseFileName() {
    String inline = MossKbDocumentPreviewService.disposition(false, "中华人民共和国行政复议法.docx");
    assertTrue(inline.startsWith("inline;filename="));
    assertTrue(inline.contains("filename*=UTF-8''"));
    assertTrue(inline.contains("%E8%A1%8C%E6%94%BF%E5%A4%8D%E8%AE%AE%E6%B3%95.docx"));
    assertTrue(MossKbDocumentPreviewService.disposition(true, null).startsWith("attachment;filename=document"));
  }

  @Test
  public void suffixIgnoresUppercaseAndMissingExtension() {
    assertEquals("docx", MossKbDocumentPreviewService.suffix("中华人民共和国行政复议法.DOCX"));
    assertEquals("", MossKbDocumentPreviewService.suffix("无后缀文件"));
    assertEquals("", MossKbDocumentPreviewService.suffix(null));
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
}
