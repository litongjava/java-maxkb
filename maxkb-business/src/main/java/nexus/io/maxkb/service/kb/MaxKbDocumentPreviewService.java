package nexus.io.maxkb.service.kb;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.jfinal.kit.Kv;

import lombok.extern.slf4j.Slf4j;
import nexus.io.db.activerecord.Db;
import nexus.io.db.activerecord.Row;
import nexus.io.model.result.ResultVo;
import nexus.io.tio.http.common.HeaderName;
import nexus.io.tio.http.common.HeaderValue;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.http.common.HttpResponseStatus;
import nexus.io.tio.http.server.util.Resps;

/**
 * 文档全文预览：把原始文件按类型翻译成浏览器能直接呈现的内容，并支持原文件下载。
 *
 * <p>PDF 与图片交给浏览器自身渲染，Office、表格与纯文本在服务端解析后返回，
 * 解析失败或原文件已被清理时退回文档分段正文，保证任何知识库文档都有可读的全文。
 */
@Slf4j
public class MaxKbDocumentPreviewService {

  /** 一次预览最多返回的正文字符数，达到上限后只保留前半部分。 */
  public static final int MAX_PREVIEW_CHARS = 200_000;

  /** 需要整份读入内存做解析的文件大小上限。 */
  private static final int MAX_PARSE_BYTES = 30 * 1024 * 1024;

  /** 表格类文档一次预览的行列上限，避免超大表格把响应撑爆。 */
  private static final int MAX_TABLE_ROWS = 2000;
  private static final int MAX_TABLE_COLUMNS = 200;

  /** 原文件没有落库时，退回分段正文的最大分段数。 */
  private static final int MAX_FALLBACK_PARAGRAPHS = 2000;

  /** 本地文件存储根目录，与静态资源目录 server.resources.static-locations 保持一致。 */
  private static final String FILE_ROOT = "pages";

  /** 预览方式：交给浏览器原生渲染。 */
  public static final String KIND_PDF = "pdf";
  public static final String KIND_IMAGE = "image";
  /** 预览方式：服务端生成的 HTML 片段，可以直接放进页面。 */
  public static final String KIND_HTML = "html";
  /** 预览方式：原始 HTML 文件，需要用沙箱 iframe 隔离渲染。 */
  public static final String KIND_WEB = "web";
  /** 预览方式：Markdown 正文，交给 Markdown 渲染器。 */
  public static final String KIND_MARKDOWN = "markdown";
  /** 预览方式：纯文本正文。 */
  public static final String KIND_TEXT = "text";
  /** 预览方式：无法在线预览，只提供下载。 */
  public static final String KIND_UNSUPPORTED = "unsupported";

  /** 下载提示：文件类型不支持在线预览。 */
  /** 无法在线预览时的提示码，由前端翻译成本地文案，服务端只给出机器可读的原因。 */
  public static final String MESSAGE_CODE_UNSUPPORTED = "unsupported";

  /** 提示码对应的中文兜底文案，供不走界面的调用方直接使用。 */
  private static final String MESSAGE_UNSUPPORTED = "该文件类型暂不支持在线预览，请下载后查看";

  private static final String FIND_APPLICATION_DOCUMENT = """
      select d.id,
             d.name,
             d.type,
             d.file_id,
             d.dataset_id,
             d.char_length,
             d.paragraph_count,
             s.name as dataset_name
        from max_kb_document d
        join max_kb_application_dataset_mapping m on m.dataset_id = d.dataset_id
   left join max_kb_dataset s on s.id = d.dataset_id
       where m.application_id = ?
         and d.id = ?
       limit 1
      """;

  private static final String FIND_FILE = """
      select filename,
             bucket_name,
             target_name,
             file_size
        from max_kb_file
       where id = ?
         and deleted = 0
      """;

  /** 分段没有单独的顺序列，主键由雪花算法递增生成，按主键升序即导入顺序。 */
  private static final String FIND_PARAGRAPHS = """
      select content
        from max_kb_paragraph
       where document_id = ?
         and deleted = 0
       order by id
       limit ?
      """;

  /** 预览结果：预览方式 + 正文 + 是否被截断。 */
  public record Preview(String kind, String content, boolean truncated) {
  }

  /**
   * 文档全文预览：返回预览方式与解析后的正文。
   *
   * <p>预览链接不需要登录，所以这里不校验请求身份：应用与文档的绑定关系就是访问凭据，
   * 只有确实挂在该应用关联知识库下的文档才会被读出来。
   *
   * @param applicationId 应用 id，用于校验文档所属知识库是否与该应用关联
   * @param documentId    文档 id
   */
  public ResultVo content(Long applicationId, Long documentId) {
    if (applicationId == null || documentId == null) {
      return ResultVo.fail("缺少应用或文档标识");
    }
    Row document = Db.findFirst(FIND_APPLICATION_DOCUMENT, applicationId, documentId);
    if (document == null) {
      return ResultVo.fail("文档不存在或不属于该应用的知识库");
    }

    Row file = file(document);
    Path path = file == null ? null : storagePath(file);
    Preview preview = path == null ? null : extract(path, document.getStr("name"));
    if (preview == null) {
      preview = segments(documentId);
    }

    Kv data = Kv.by("document_id", document.getLong("id"))
        //
        .set("document_name", document.getStr("name"))
        //
        .set("document_type", document.getStr("type"))
        //
        .set("dataset_id", document.getLong("dataset_id"))
        //
        .set("dataset_name", document.getStr("dataset_name"))
        //
        .set("paragraph_count", document.getInt("paragraph_count"))
        //
        .set("char_length", document.getInt("char_length"))
        //
        .set("file_name", file == null ? null : file.getStr("filename"))
        //
        .set("file_size", file == null ? null : file.getLong("file_size"))
        //
        .set("downloadable", path != null)
        //
        .set("preview_kind", preview.kind())
        //
        .set("content", preview.content())
        //
        .set("truncated", preview.truncated())
        //
        .set("message_code", KIND_UNSUPPORTED.equals(preview.kind()) ? MESSAGE_CODE_UNSUPPORTED : null)
        //
        .set("message", KIND_UNSUPPORTED.equals(preview.kind()) ? MESSAGE_UNSUPPORTED : null);
    return ResultVo.ok(data);
  }

  /**
   * 原文件输出：预览时按 inline 返回，下载时按 attachment 返回。
   *
   * <p>与全文预览一样不要求登录，只有挂在该应用关联知识库下的文档才会被读出来。
   *
   * @param download true 表示按附件下载，浏览器会弹出保存
   */
  public HttpResponse file(Long applicationId, Long documentId, boolean download, HttpRequest request) {
    if (applicationId == null || documentId == null) {
      return json(request, ResultVo.fail("缺少应用或文档标识"), HttpResponseStatus.C404);
    }
    Row document = Db.findFirst(FIND_APPLICATION_DOCUMENT, applicationId, documentId);
    if (document == null) {
      return json(request, ResultVo.fail("文档不存在或不属于该应用的知识库"), HttpResponseStatus.C404);
    }
    Row file = file(document);
    Path path = file == null ? null : storagePath(file);
    if (path == null) {
      return json(request, ResultVo.fail("原文件不存在或已被清理"), HttpResponseStatus.C404);
    }
    try {
      byte[] bytes = Files.readAllBytes(path);
      String filename = document.getStr("name");
      HttpResponse response = Resps.bytesWithContentType(request, bytes, contentType(suffix(filename)));
      response.addHeader(HeaderName.Content_Disposition, HeaderValue.from(disposition(download, filename)));
      return response;
    } catch (Exception e) {
      log.warn("文档原文件读取失败, documentId={}, path={}", documentId, path, e);
      return json(request, ResultVo.fail("原文件读取失败"), HttpResponseStatus.C500);
    }
  }

  /**
   * 按文件名后缀选择预览方式，返回 null 表示原文件不可读、由调用方退回分段正文。
   */
  private Preview extract(Path path, String documentName) {
    try {
      if (!Files.isReadable(path) || Files.size(path) == 0) {
        return null;
      }
      long size = Files.size(path);
      if (size > MAX_PARSE_BYTES) {
        return new Preview(KIND_UNSUPPORTED, null, false);
      }
      return switch (suffix(documentName)) {
        case "pdf" -> new Preview(KIND_PDF, null, false);
        case "png", "jpg", "jpeg", "gif", "bmp", "webp", "svg" -> new Preview(KIND_IMAGE, null, false);
        case "docx" -> html(docxToHtml(Files.readAllBytes(path)));
        case "doc" -> html(docToHtml(Files.readAllBytes(path)));
        case "xlsx", "xls" -> spreadsheetToHtml(Files.readAllBytes(path));
        case "csv" -> csvToHtml(readText(Files.readAllBytes(path)));
        case "md", "markdown" -> limited(KIND_MARKDOWN, readText(Files.readAllBytes(path)), false);
        case "html", "htm" -> limited(KIND_WEB, readText(Files.readAllBytes(path)), false);
        case "zip" -> limited(KIND_TEXT, zipListing(Files.readAllBytes(path)), false);
        default -> textLike(documentName) ? limited(KIND_TEXT, readText(Files.readAllBytes(path)), false)
            : new Preview(KIND_UNSUPPORTED, null, false);
      };
    } catch (Exception e) {
      log.warn("文档预览解析失败, path={}", path, e);
      return new Preview(KIND_UNSUPPORTED, null, false);
    }
  }

  /**
   * 原文件缺失时的兜底：把全部分段按顺序拼成 Markdown 正文，仍然看得到文档全文。
   */
  private Preview segments(Long documentId) {
    List<Row> records = Db.find(FIND_PARAGRAPHS, documentId, MAX_FALLBACK_PARAGRAPHS);
    StringBuilder text = new StringBuilder();
    boolean truncated = false;
    for (Row record : records) {
      String content = record.getStr("content");
      if (content == null || content.isBlank()) {
        continue;
      }
      if (text.length() + content.length() > MAX_PREVIEW_CHARS) {
        truncated = true;
        break;
      }
      text.append(content).append("\n\n");
    }
    if (text.length() == 0) {
      return new Preview(KIND_UNSUPPORTED, null, false);
    }
    return new Preview(KIND_MARKDOWN, text.toString(), truncated);
  }

  private Row file(Row document) {
    Long fileId = document.getLong("file_id");
    if (fileId == null) {
      return null;
    }
    return Db.findFirst(FIND_FILE, fileId);
  }

  /** 原文件落盘路径：pages/{bucketName}/{targetName}。 */
  private Path storagePath(Row file) {
    String bucketName = file.getStr("bucket_name");
    String targetName = file.getStr("target_name");
    if (bucketName == null || targetName == null) {
      return null;
    }
    Path path = Paths.get(FILE_ROOT, bucketName, targetName).normalize();
    if (!path.startsWith(FILE_ROOT) || !Files.isReadable(path)) {
      return null;
    }
    return path;
  }

  /**
   * DOCX 正文转 HTML：段落转 p，表格转 table，文本统一做转义，避免文档内容被当成标签执行。
   */
  public String docxToHtml(byte[] data) throws Exception {
    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(data))) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        if (!"word/document.xml".equals(entry.getName())) {
          continue;
        }
        byte[] xml = zip.readNBytes(20 * 1024 * 1024 + 1);
        if (xml.length > 20 * 1024 * 1024) {
          throw new IllegalArgumentException("DOCX内容过大");
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        Node body = doc.getElementsByTagNameNS("*", "body").item(0);
        if (body == null) {
          throw new IllegalArgumentException("DOCX缺少正文");
        }
        StringBuilder out = new StringBuilder();
        for (Node node = body.getFirstChild(); node != null; node = node.getNextSibling()) {
          if ("p".equals(node.getLocalName())) {
            appendParagraph(out, nodeText(node));
          } else if ("tbl".equals(node.getLocalName())) {
            out.append("<table class=\"preview-table\">");
            NodeList rows = ((Element) node).getElementsByTagNameNS("*", "tr");
            for (int r = 0; r < rows.getLength(); r++) {
              NodeList cells = ((Element) rows.item(r)).getElementsByTagNameNS("*", "tc");
              out.append("<tr>");
              for (int c = 0; c < cells.getLength(); c++) {
                out.append("<td>").append(escape(nodeText(cells.item(c)))).append("</td>");
              }
              out.append("</tr>");
            }
            out.append("</table>");
          }
        }
        return out.toString();
      }
    }
    throw new IllegalArgumentException("DOCX缺少正文");
  }

  /** DOC 正文转 HTML：老式二进制 Word 只能逐段取文本。 */
  public String docToHtml(byte[] data) throws Exception {
    try (HWPFDocument word = new HWPFDocument(new ByteArrayInputStream(data));
        WordExtractor extractor = new WordExtractor(word)) {
      StringBuilder out = new StringBuilder();
      for (String paragraph : extractor.getParagraphText()) {
        appendParagraph(out, paragraph.replaceAll("[\\r\\n\\u0007]", ""));
      }
      return out.toString();
    }
  }

  private void appendParagraph(StringBuilder out, String text) {
    if (text == null || text.isBlank()) {
      out.append("<p class=\"preview-blank\">&nbsp;</p>");
      return;
    }
    out.append("<p>").append(escape(text)).append("</p>");
  }

  /** 表格文档转 HTML 表格，超过行列上限时截断并标记。 */
  public Preview spreadsheetToHtml(byte[] data) throws Exception {
    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(data))) {
      DataFormatter formatter = new DataFormatter(Locale.ROOT);
      formatter.setUseCachedValuesForFormulaCells(true);
      StringBuilder out = new StringBuilder();
      boolean truncated = false;
      for (Sheet sheet : workbook) {
        out.append("<h4>").append(escape(sheet.getSheetName())).append("</h4>");
        out.append("<table class=\"preview-table\">");
        int rows = 0;
        for (org.apache.poi.ss.usermodel.Row row : sheet) {
          if (rows >= MAX_TABLE_ROWS) {
            truncated = true;
            break;
          }
          rows++;
          int lastCell = row.getLastCellNum();
          if (lastCell > MAX_TABLE_COLUMNS) {
            truncated = true;
          }
          out.append("<tr>");
          for (int i = 0; i < Math.min(lastCell, MAX_TABLE_COLUMNS); i++) {
            Cell cell = row.getCell(i);
            out.append("<td>").append(escape(formatter.formatCellValue(cell))).append("</td>");
          }
          out.append("</tr>");
        }
        out.append("</table>");
      }
      return limited(KIND_HTML, out.toString(), truncated);
    }
  }

  /** CSV 转 HTML 表格，按 RFC4180 处理双引号包裹的单元格。 */
  public Preview csvToHtml(String text) {
    StringBuilder out = new StringBuilder("<table class=\"preview-table\">");
    boolean truncated = false;
    int rows = 0;
    for (String line : text.split("\\r?\\n")) {
      if (line.isBlank()) {
        continue;
      }
      if (rows >= MAX_TABLE_ROWS) {
        truncated = true;
        break;
      }
      rows++;
      out.append("<tr>");
      for (String cell : splitCsvLine(line)) {
        out.append("<td>").append(escape(cell)).append("</td>");
      }
      out.append("</tr>");
    }
    out.append("</table>");
    return limited(KIND_HTML, out.toString(), truncated);
  }

  /** 单个 CSV 行切分：引号内的逗号属于单元格内容，两个连续引号表示一个引号。 */
  public static List<String> splitCsvLine(String line) {
    List<String> cells = new ArrayList<>();
    StringBuilder cell = new StringBuilder();
    boolean quoted = false;
    for (int i = 0; i < line.length(); i++) {
      char current = line.charAt(i);
      if (current == '"') {
        if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
          cell.append('"');
          i++;
        } else {
          quoted = !quoted;
        }
      } else if (current == ',' && !quoted) {
        cells.add(cell.toString());
        cell.setLength(0);
      } else {
        cell.append(current);
      }
    }
    cells.add(cell.toString());
    return cells;
  }

  /** ZIP 没有可直接渲染的视图，至少把包内清单列出来。 */
  public String zipListing(byte[] data) throws Exception {
    StringBuilder out = new StringBuilder();
    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(data))) {
      ZipEntry entry;
      int count = 0;
      while ((entry = zip.getNextEntry()) != null) {
        if (++count > 500) {
          out.append("\n...");
          break;
        }
        if (entry.isDirectory()) {
          continue;
        }
        out.append(entry.getName()).append("  ").append(entry.getSize() < 0 ? "" : entry.getSize() + " B").append('\n');
      }
    }
    return out.toString();
  }

  /** 文本文件按 UTF-8 读取，乱码时退回 GB18030，覆盖 Windows 下常见的记事本文件。 */
  public static String readText(byte[] data) {
    try {
      return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(data)).toString();
    } catch (CharacterCodingException e) {
      return new String(data, Charset.forName("GB18030"));
    }
  }

  /** 纯文本类后缀，这些文件不需要解析，直接按文本返回。 */
  private static boolean textLike(String documentName) {
    return documentName.toLowerCase(Locale.ROOT)
        .matches(".*\\.(txt|log|json|xml|yaml|yml|properties|sql|java|js|ts|vue|py|sh|bat|ini|conf)$");
  }

  private Preview html(String content) {
    return limited(KIND_HTML, content, false);
  }

  /** 按字符上限截断正文，命中上限时由前端提示用户下载原文件查看。 */
  private Preview limited(String kind, String content, boolean truncated) {
    if (content != null && content.length() > MAX_PREVIEW_CHARS) {
      return new Preview(kind, content.substring(0, MAX_PREVIEW_CHARS), true);
    }
    return new Preview(kind, content, truncated);
  }

  public static String suffix(String filename) {
    if (filename == null) {
      return "";
    }
    int index = filename.lastIndexOf('.');
    return index < 0 ? "" : filename.substring(index + 1).toLowerCase(Locale.ROOT);
  }

  /** 原文件输出的 Content-Type，取不到的类型按二进制流返回，只用于下载。 */
  public static String contentType(String suffix) {
    return switch (suffix) {
      case "pdf" -> "application/pdf";
      case "png" -> "image/png";
      case "jpg", "jpeg" -> "image/jpeg";
      case "gif" -> "image/gif";
      case "bmp" -> "image/bmp";
      case "webp" -> "image/webp";
      case "svg" -> "image/svg+xml";
      case "html", "htm" -> "text/html;charset=utf-8";
      case "txt", "log", "md", "markdown", "csv", "json", "xml" -> "text/plain;charset=utf-8";
      default -> "application/octet-stream";
    };
  }

  /** 预览用 inline，下载用 attachment；文件名按 UTF-8 百分号编码，兼容中文名。 */
  public static String disposition(boolean download, String filename) {
    String name = filename == null || filename.isBlank() ? "document" : filename;
    String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
    return (download ? "attachment" : "inline") + ";filename=" + encoded + ";filename*=UTF-8''" + encoded;
  }

  /** 文档正文里的 & < > " 必须转义，否则文档内容会被浏览器当成标签执行。 */
  public static String escape(String text) {
    if (text == null) {
      return "";
    }
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
  }

  /** 取节点下所有文本，DOCX 的文本可能被拆在多个 w:t 里。 */
  private String nodeText(Node node) {
    StringBuilder out = new StringBuilder();
    NodeList texts = ((Element) node).getElementsByTagNameNS("*", "t");
    for (int i = 0; i < texts.getLength(); i++) {
      out.append(texts.item(i).getTextContent());
    }
    return out.toString();
  }

  private HttpResponse json(HttpRequest request, ResultVo body, HttpResponseStatus status) {
    HttpResponse response = Resps.json(request, body);
    response.setStatus(status);
    return response;
  }
}
