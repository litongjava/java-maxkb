package nexus.io.maxkb.service.kb;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import lombok.extern.slf4j.Slf4j;

/**
 * 抓取 Web 站点页面并转换为 Markdown，再按标题切分为分段。
 *
 * <p>
 * 只使用一个入口地址：先抓入口页面，再按同目录前缀逐层抓取子链接。页面正文默认取
 * body，也可以用 CSS 选择器缩小正文范围。转换出的 Markdown 保留标题层级，分段时按
 * 标题切分，因此每个分段都带有标题链。
 */
@Slf4j
public class WebCrawlService {

  /** 部分站点会过滤默认 UA，因此使用浏览器 UA。 */
  public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36";

  /** 单页抓取超时时间。 */
  private static final int TIMEOUT_MS = 20_000;

  /** 单页最大响应体积，超过后 jsoup 直接放弃解析。 */
  private static final int MAX_BODY_BYTES = 5 * 1024 * 1024;

  /** 入口页面之下允许跟随的子链接层数，0 表示只抓入口页面。 */
  private static final int MAX_DEPTH = 2;

  /** 一次同步最多抓取的页面数，防止站点层级过深时无限扩散。 */
  private static final int MAX_PAGES = 50;

  /** 单个分段的最大字符数，超长小节继续按行切分。 */
  private static final int MAX_PARAGRAPH_CHARS = 2000;

  /** 文档名称上限，与知识库其它入口保持一致。 */
  private static final int MAX_NAME_CHARS = 128;

  /** 非正文节点，转换 Markdown 时整棵跳过。 */
  private static final Set<String> SKIPPED_TAGS = Set.of("script", "style", "noscript", "template", "svg", "canvas",
      "iframe", "head", "form", "select", "option", "button", "nav", "footer", "aside");

  /** 列表项内出现这些标签时按块级内容展开，而不是压成一行。 */
  private static final Set<String> NESTED_TAGS = Set.of("ul", "ol", "table", "pre", "div", "p");

  private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.*)$");

  /** 一个被抓取的页面。 */
  public record Page(String url, String name, String markdown, List<String> links) {
  }

  /** 一个分段：标题链加正文。 */
  public record Paragraph(String title, String content) {
  }

  /** 地址不可用或页面无法解析时抛出，调用方据此给出可读的失败提示。 */
  public static class CrawlException extends IOException {
    private static final long serialVersionUID = 1L;

    public CrawlException(String message) {
      super(message);
    }
  }

  /**
   * 从入口地址开始抓取，逐层展开同目录前缀下的子链接，每抓到一页立即交给回调。
   *
   * @param sourceUrl 入口地址
   * @param selector  正文选择器，为空时取整个 body
   * @param firstPage 已经抓取过的入口页面，可为空；不为空时不再重复请求入口地址
   * @param onPage    每抓到一个页面就回调一次，由调用方决定如何落库
   * @return 实际抓取到的页面数，包含入口页面
   */
  public int crawl(String sourceUrl, String selector, Page firstPage, Consumer<Page> onPage) throws IOException {
    String entry = absoluteUrl(sourceUrl);
    if (entry == null) {
      throw new CrawlException("Web 根地址必须是 http 或 https 地址");
    }
    String prefix = directoryPrefix(entry);
    Set<String> visited = new LinkedHashSet<>();
    Deque<Object[]> queue = new ArrayDeque<>();
    int count = 0;
    if (firstPage != null) {
      count++;
      visited.add(trimSlash(firstPage.url()));
      onPage.accept(firstPage);
      for (String link : firstPage.links()) {
        if (link.startsWith(prefix)) {
          queue.add(new Object[] { link, 1 });
        }
      }
    } else {
      queue.add(new Object[] { entry, 0 });
    }
    while (!queue.isEmpty() && count < MAX_PAGES) {
      Object[] current = queue.poll();
      String url = (String) current[0];
      int depth = (Integer) current[1];
      if (url == null || !visited.add(trimSlash(url))) {
        continue;
      }
      Page page;
      try {
        page = fetch(url, selector);
      } catch (IOException e) {
        log.warn("抓取页面失败 url={} message={}", url, e.getMessage());
        continue;
      }
      // 站点常用重定向把带后缀的地址指向正式地址，同一页面只保留一份。
      String canonicalKey = trimSlash(page.url());
      if (!canonicalKey.equals(trimSlash(url)) && !visited.add(canonicalKey)) {
        continue;
      }
      count++;
      onPage.accept(page);
      if (depth >= MAX_DEPTH) {
        continue;
      }
      for (String link : page.links()) {
        if (link.startsWith(prefix) && !visited.contains(trimSlash(link))) {
          queue.add(new Object[] { link, depth + 1 });
        }
      }
    }
    if (count >= MAX_PAGES) {
      log.info("一次同步达到页面数量上限 pages={} sourceUrl={}", count, sourceUrl);
    }
    return count;
  }

  /** 抓取并汇总返回，主要用于测试。 */
  public List<Page> crawl(String sourceUrl, String selector, Page firstPage) throws IOException {
    List<Page> pages = new ArrayList<>();
    crawl(sourceUrl, selector, firstPage, pages::add);
    return pages;
  }

  /** 抓取单个页面，链接只用于继续扩散，不计入正文。 */
  public Page fetch(String url, String selector) throws IOException {
    String target = absoluteUrl(url);
    if (target == null) {
      throw new CrawlException("Web 根地址必须是 http 或 https 地址");
    }
    Connection.Response response;
    try {
      response = Jsoup.connect(target).userAgent(USER_AGENT).timeout(TIMEOUT_MS).maxBodySize(MAX_BODY_BYTES)
          .followRedirects(true).ignoreHttpErrors(true).execute();
    } catch (IOException e) {
      throw new CrawlException("无法访问该地址：" + e.getMessage());
    }
    if (response.statusCode() != 200) {
      throw new CrawlException("目标地址返回 HTTP " + response.statusCode());
    }
    Document document;
    try {
      document = response.parse();
    } catch (IOException e) {
      throw new CrawlException("页面内容无法解析：" + e.getMessage());
    }
    // 记录重定向之后的正式地址，避免同一页面按多个地址重复入库。
    String finalUrl = absoluteUrl(response.url() == null ? target : response.url().toString());
    if (finalUrl == null) {
      finalUrl = target;
    }
    return new Page(finalUrl, pageName(document, finalUrl), markdown(document, selector), pageLinks(document));
  }

  /** 选择器命中多个元素时按出现顺序拼接正文；选择器没有命中时回退到 body。 */
  static String markdown(Document document, String selector) {
    Elements elements;
    if (selector == null || selector.isBlank()) {
      elements = new Elements();
    } else {
      try {
        elements = document.select(selector);
      } catch (RuntimeException e) {
        throw new IllegalArgumentException("选择器格式不正确：" + selector, e);
      }
    }
    if (elements.isEmpty()) {
      Element body = document.body();
      return toMarkdown(body == null ? document : body, document.baseUri());
    }
    StringBuilder out = new StringBuilder();
    for (Element element : elements) {
      out.append(toMarkdown(element, document.baseUri())).append("\n\n");
    }
    return tidy(out.toString());
  }

  static String toMarkdown(Element root, String baseUri) {
    StringBuilder out = new StringBuilder();
    appendChildren(root, out, baseUri);
    return tidy(out.toString());
  }

  static String tidy(String markdown) {
    return markdown.replaceAll("[ \\t]+\\n", "\n").replaceAll("\\n{3,}", "\n\n").strip();
  }

  private static void appendChildren(Element parent, StringBuilder out, String baseUri) {
    for (Node node : parent.childNodes()) {
      appendNode(node, out, baseUri);
    }
  }

  private static void appendNode(Node node, StringBuilder out, String baseUri) {
    if (node instanceof TextNode textNode) {
      out.append(textNode.getWholeText().replace('\u00a0', ' '));
      return;
    }
    if (!(node instanceof Element element)) {
      return;
    }
    String tag = element.tagName().toLowerCase(Locale.ROOT);
    if (SKIPPED_TAGS.contains(tag) || element.hasAttr("hidden")) {
      return;
    }
    switch (tag) {
      case "br" -> out.append('\n');
      case "hr" -> out.append("\n\n---\n\n");
      case "h1", "h2", "h3", "h4", "h5", "h6" -> {
        int level = tag.charAt(1) - '0';
        out.append("\n\n").append("#".repeat(level)).append(' ').append(inline(element, baseUri)).append("\n\n");
      }
      case "p", "div", "section", "article", "main", "header" -> {
        out.append("\n\n");
        appendChildren(element, out, baseUri);
        out.append("\n\n");
      }
      case "blockquote" -> {
        out.append("\n\n");
        for (String line : blockText(element, baseUri).split("\n")) {
          out.append("> ").append(line).append('\n');
        }
        out.append('\n');
      }
      case "ul", "ol" -> appendList(element, out, baseUri, tag);
      case "pre" -> out.append("\n\n```\n").append(element.wholeText().strip()).append("\n```\n\n");
      case "table" -> out.append("\n\n").append(table(element, baseUri)).append("\n\n");
      case "strong", "b" -> out.append("**").append(inline(element, baseUri)).append("**");
      case "em", "i" -> out.append('*').append(inline(element, baseUri)).append('*');
      case "code" -> out.append('`').append(element.text()).append('`');
      case "a" -> appendLink(element, out, baseUri);
      case "img" -> appendImage(element, out);
      default -> appendChildren(element, out, baseUri);
    }
  }

  private static void appendList(Element list, StringBuilder out, String baseUri, String tag) {
    int index = 0;
    for (Element item : list.children()) {
      if (!"li".equalsIgnoreCase(item.tagName())) {
        appendChildren(item, out, baseUri);
        continue;
      }
      index++;
      out.append('\n').append("ol".equals(tag) ? index + ". " : "- ");
      if (item.select(String.join(",", NESTED_TAGS)).isEmpty()) {
        out.append(inline(item, baseUri));
      } else {
        out.append(toMarkdown(item, baseUri).replace("\n", "\n  "));
      }
    }
    out.append("\n\n");
  }

  private static void appendLink(Element element, StringBuilder out, String baseUri) {
    String text = inline(element, baseUri);
    if (text.isEmpty()) {
      return;
    }
    String href = absoluteUrl(element.absUrl("href"));
    if (href == null) {
      out.append(text);
      return;
    }
    out.append('[').append(text).append("](").append(href).append(')');
  }

  private static void appendImage(Element element, StringBuilder out) {
    String src = absoluteUrl(element.absUrl("src"));
    if (src == null) {
      return;
    }
    String alt = element.attr("alt").replace("[", "").replace("]", "").replace("\n", " ");
    out.append("![").append(alt).append("](").append(src).append(')');
  }

  private static String table(Element table, String baseUri) {
    List<List<String>> rows = new ArrayList<>();
    for (Element row : table.select("tr")) {
      List<String> cells = new ArrayList<>();
      for (Element cell : row.children()) {
        String tag = cell.tagName().toLowerCase(Locale.ROOT);
        if ("td".equals(tag) || "th".equals(tag)) {
          cells.add(inline(cell, baseUri).replace("|", "\\|"));
        }
      }
      if (!cells.isEmpty()) {
        rows.add(cells);
      }
    }
    if (rows.isEmpty()) {
      return "";
    }
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < rows.size(); i++) {
      out.append('|').append(String.join("|", rows.get(i))).append("|\n");
      if (i == 0) {
        out.append('|');
        for (int c = 0; c < rows.get(0).size(); c++) {
          out.append("---|");
        }
        out.append('\n');
      }
    }
    return out.toString();
  }

  /** 把元素渲染成单行文本，用于标题、单元格、列表项等行内位置。 */
  private static String inline(Element element, String baseUri) {
    StringBuilder out = new StringBuilder();
    appendChildren(element, out, baseUri);
    return out.toString().replaceAll("\\s+", " ").strip();
  }

  /** 块级内容渲染成多行文本，用于引用块。 */
  private static String blockText(Element element, String baseUri) {
    StringBuilder out = new StringBuilder();
    appendChildren(element, out, baseUri);
    return tidy(out.toString());
  }

  /** 按标题切分 Markdown：标题构成标题链，正文构成分段内容。 */
  public List<Paragraph> split(String markdown) {
    List<Paragraph> paragraphs = new ArrayList<>();
    if (markdown == null || markdown.isBlank()) {
      return paragraphs;
    }
    String normalized = markdown.replace("\r\n", "\n").replace('\r', '\n');
    List<String> chain = new ArrayList<>();
    List<String> sectionTitles = List.of();
    StringBuilder body = new StringBuilder();
    for (String line : normalized.split("\n", -1)) {
      Matcher matcher = HEADING.matcher(line.strip());
      if (!matcher.matches()) {
        body.append(line).append('\n');
        continue;
      }
      appendSection(paragraphs, sectionTitles, body);
      body.setLength(0);
      int level = matcher.group(1).length();
      while (chain.size() >= level) {
        chain.remove(chain.size() - 1);
      }
      chain.add(matcher.group(2).strip());
      sectionTitles = List.copyOf(chain);
    }
    appendSection(paragraphs, sectionTitles, body);
    return paragraphs;
  }

  private void appendSection(List<Paragraph> paragraphs, List<String> titles, StringBuilder body) {
    String content = body.toString().strip();
    String title = String.join(" ", titles);
    if (content.isEmpty()) {
      // 只有标题没有正文的小节保留标题，避免目录型页面整页丢失。
      if (!title.isEmpty()) {
        paragraphs.add(new Paragraph("", title));
      }
      return;
    }
    for (String chunk : splitLong(content)) {
      paragraphs.add(new Paragraph(title, chunk));
    }
  }

  private List<String> splitLong(String content) {
    List<String> chunks = new ArrayList<>();
    if (content.length() <= MAX_PARAGRAPH_CHARS) {
      chunks.add(content);
      return chunks;
    }
    StringBuilder current = new StringBuilder();
    for (String line : content.split("\n", -1)) {
      if (line.length() > MAX_PARAGRAPH_CHARS) {
        if (current.length() > 0) {
          chunks.add(current.toString().strip());
          current.setLength(0);
        }
        for (int start = 0; start < line.length(); start += MAX_PARAGRAPH_CHARS) {
          chunks.add(line.substring(start, Math.min(line.length(), start + MAX_PARAGRAPH_CHARS)));
        }
        continue;
      }
      if (current.length() > 0 && current.length() + line.length() + 1 > MAX_PARAGRAPH_CHARS) {
        chunks.add(current.toString().strip());
        current.setLength(0);
      }
      current.append(line).append('\n');
    }
    if (current.length() > 0) {
      chunks.add(current.toString().strip());
    }
    return chunks.stream().filter(chunk -> !chunk.isBlank()).toList();
  }

  /** 收集页面里的 http(s) 链接，已做绝对化与去重。 */
  static List<String> pageLinks(Document document) {
    List<String> links = new ArrayList<>();
    Set<String> seen = new LinkedHashSet<>();
    for (Element anchor : document.select("a[href]")) {
      String href = absoluteUrl(anchor.absUrl("href"));
      if (href != null && seen.add(href)) {
        links.add(href);
      }
    }
    return links;
  }

  private static String pageName(Document document, String url) {
    String title = document.title() == null ? "" : document.title().strip();
    String name = title.isEmpty() ? url : title;
    return name.length() > MAX_NAME_CHARS ? name.substring(0, MAX_NAME_CHARS) : name;
  }

  /** 规范化地址：只保留 http(s)，去掉片段与结尾斜杠。 */
  static String absoluteUrl(String url) {
    if (url == null || url.isBlank()) {
      return null;
    }
    String value = url.strip();
    try {
      URI uri = new URI(value);
      String scheme = uri.getScheme() == null ? null : uri.getScheme().toLowerCase(Locale.ROOT);
      if (!"http".equals(scheme) && !"https".equals(scheme)) {
        return null;
      }
      if (uri.getHost() == null) {
        return null;
      }
    } catch (URISyntaxException e) {
      // 带中文等未编码字符的地址无法交给 URI 解析，退化为前缀判断。
      String lower = value.toLowerCase(Locale.ROOT);
      if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
        return null;
      }
      int hostEnd = value.indexOf('/', value.indexOf("//") + 2);
      if (hostEnd == value.indexOf("//") + 2) {
        return null;
      }
    }
    String withoutFragment = value.split("#")[0];
    return withoutFragment.endsWith("/") ? withoutFragment.substring(0, withoutFragment.length() - 1) : withoutFragment;
  }

  /** 入口地址所在目录，子链接必须落在该前缀下。 */
  static String directoryPrefix(String entryUrl) {
    int index = entryUrl.lastIndexOf('/');
    int schemeIndex = entryUrl.indexOf("//");
    if (index <= schemeIndex + 1) {
      return entryUrl;
    }
    return entryUrl.substring(0, index);
  }

  private static String trimSlash(String url) {
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }
}
