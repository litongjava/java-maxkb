package nexus.io.mosskb.service.kb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import nexus.io.mosskb.service.kb.WebCrawlService.Page;
import nexus.io.mosskb.service.kb.WebCrawlService.Paragraph;

public class WebCrawlServiceTest {

  private HttpServer server;
  private String base;
  private final Set<String> requested = ConcurrentHashMap.newKeySet();

  @Before
  public void start() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    base = "http://127.0.0.1:" + server.getAddress().getPort();
    handle("/docs/index.html", """
        <html><head><title>文档首页</title></head><body>
        <nav><a href="/docs/nav.html">导航</a></nav>
        <main>
        <h1>快速开始</h1><p>先安装依赖。</p>
        <ul><li>第一步</li><li>第二步</li></ul>
        <h2>配置</h2>
        <table><tr><th>参数</th><th>说明</th></tr><tr><td>port</td><td>服务端口</td></tr></table>
        <pre><code>java -jar app.jar</code></pre>
        <p><a href="/docs/a.html">子页面</a> <a href="/other/x.html">站外目录</a></p>
        </main>
        <div id="foot">页脚内容</div>
        <script>var hidden = 1;</script>
        </body></html>
        """);
    handle("/docs/a.html", "<html><head><title>子页面 A</title></head><body><h1>A</h1><p>A 正文</p>"
        + "<a href=\"/docs/b.html\">下一层</a></body></html>");
    handle("/docs/b.html", "<html><head><title>子页面 B</title></head><body><h1>B</h1><p>B 正文</p>"
        + "<a href=\"/docs/c.html\">第三层</a></body></html>");
    handle("/docs/c.html", "<html><head><title>子页面 C</title></head><body><h1>C</h1><p>C 正文</p></body></html>");
    handle("/docs/nav.html", "<html><head><title>导航页</title></head><body><h1>导航页</h1></body></html>");
    handle("/other/x.html", "<html><head><title>站外</title></head><body><h1>站外</h1></body></html>");
    // 带后缀的地址重定向到正式地址，两种写法指向同一个页面
    server.createContext("/docs/redir.html", exchange -> {
      requested.add(exchange.getRequestURI().getPath());
      exchange.getResponseHeaders().set("Location", "/docs/redir");
      exchange.sendResponseHeaders(308, -1);
      exchange.close();
    });
    handle("/docs/redir", "<html><head><title>重定向页</title></head><body><h1>重定向页</h1><p>正文</p></body></html>");
    handle("/docs/redirect-index.html",
        "<html><head><title>重定向入口</title></head><body><h1>重定向入口</h1>"
            + "<a href=\"/docs/redir.html\">带后缀</a><a href=\"/docs/redir\">正式地址</a></body></html>");
    server.start();
  }

  @After
  public void stop() {
    server.stop(0);
  }

  private void handle(String path, String html) {
    server.createContext(path, exchange -> {
      requested.add(exchange.getRequestURI().getPath());
      respond(exchange, html);
    });
  }

  private static void respond(HttpExchange exchange, String html) throws java.io.IOException {
    byte[] body = html.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
    exchange.sendResponseHeaders(200, body.length);
    exchange.getResponseBody().write(body);
    exchange.close();
  }

  @Test
  public void fetchConvertsPageToMarkdown() throws Exception {
    Page page = new WebCrawlService().fetch(base + "/docs/index.html", "");
    assertEquals("文档首页", page.name());
    String markdown = page.markdown();
    assertTrue(markdown.contains("# 快速开始"));
    assertTrue(markdown.contains("## 配置"));
    assertTrue(markdown.contains("- 第一步"));
    assertTrue(markdown.contains("|参数|说明|"));
    assertTrue(markdown.contains("|port|服务端口|"));
    assertTrue(markdown.contains("```\njava -jar app.jar\n```"));
    assertTrue(markdown.contains("[子页面](" + base + "/docs/a.html)"));
    assertTrue(markdown.contains("页脚内容"));
    // 导航、脚本属于非正文节点
    assertFalse(markdown.contains("导航"));
    assertFalse(markdown.contains("var hidden"));
    assertTrue(page.links().contains(base + "/docs/a.html"));
    assertTrue(page.links().contains(base + "/other/x.html"));
  }

  @Test
  public void selectorNarrowsTheContent() throws Exception {
    String markdown = new WebCrawlService().fetch(base + "/docs/index.html", "main").markdown();
    assertTrue(markdown.contains("先安装依赖"));
    assertFalse(markdown.contains("页脚内容"));
  }

  @Test
  public void unknownSelectorFallsBackToBody() throws Exception {
    String markdown = new WebCrawlService().fetch(base + "/docs/index.html", ".not-exists").markdown();
    assertTrue(markdown.contains("先安装依赖"));
    assertTrue(markdown.contains("页脚内容"));
  }

  @Test
  public void crawlKeepsSameDirectoryAndDepthLimit() throws Exception {
    List<Page> pages = new WebCrawlService().crawl(base + "/docs/index.html", "", null);
    List<String> urls = new ArrayList<>();
    for (Page page : pages) {
      urls.add(page.url());
    }
    assertEquals(4, pages.size());
    assertTrue(urls.contains(base + "/docs/index.html"));
    assertTrue(urls.contains(base + "/docs/a.html"));
    assertTrue(urls.contains(base + "/docs/b.html"));
    // 导航节点不进正文，但其中的链接仍然会被跟随
    assertTrue(urls.contains(base + "/docs/nav.html"));
    // 第三层链接与同站其它目录都不再抓取
    assertFalse(requested.contains("/docs/c.html"));
    assertFalse(requested.contains("/other/x.html"));
  }

  @Test
  public void crawlWithPrefetchedEntryStillKeepsSameDirectory() throws Exception {
    WebCrawlService crawler = new WebCrawlService();
    Page firstPage = crawler.fetch(base + "/docs/index.html", "");
    List<Page> pages = crawler.crawl(base + "/docs/index.html", "", firstPage);
    List<String> urls = new ArrayList<>();
    for (Page page : pages) {
      urls.add(page.url());
      assertTrue(page.url().startsWith(base + "/docs/"));
    }
    assertTrue(urls.contains(base + "/docs/a.html"));
    assertFalse(requested.contains("/other/x.html"));
  }

  @Test
  public void redirectTargetIsKeptOnlyOnce() throws Exception {
    List<Page> pages = new WebCrawlService().crawl(base + "/docs/redirect-index.html", "", null);
    List<String> urls = new ArrayList<>();
    for (Page page : pages) {
      urls.add(page.url());
    }
    assertEquals(2, pages.size());
    assertTrue(urls.contains(base + "/docs/redirect-index.html"));
    // 重定向之后的正式地址入库，带后缀的地址不再单独成页
    assertTrue(urls.contains(base + "/docs/redir"));
    assertFalse(urls.contains(base + "/docs/redir.html"));
  }

  @Test
  public void splitKeepsHeadingChainAsTitle() {
    String markdown = """
        # 一级标题

        正文一

        ## 二级标题

        正文二

        ### 三级标题

        正文三

        ## 只有标题
        """;
    List<Paragraph> paragraphs = new WebCrawlService().split(markdown);
    assertEquals(4, paragraphs.size());
    assertEquals("一级标题", paragraphs.get(0).title());
    assertEquals("正文一", paragraphs.get(0).content());
    assertEquals("一级标题 二级标题", paragraphs.get(1).title());
    assertEquals("正文二", paragraphs.get(1).content());
    assertEquals("一级标题 二级标题 三级标题", paragraphs.get(2).title());
    assertEquals("正文三", paragraphs.get(2).content());
    assertEquals("", paragraphs.get(3).title());
    assertEquals("一级标题 只有标题", paragraphs.get(3).content());
  }

  @Test
  public void splitBreaksLongSections() {
    StringBuilder markdown = new StringBuilder("# 长文\n\n");
    for (int i = 0; i < 400; i++) {
      markdown.append("第").append(i).append("行内容。\n");
    }
    List<Paragraph> paragraphs = new WebCrawlService().split(markdown.toString());
    assertTrue(paragraphs.size() > 1);
    for (Paragraph paragraph : paragraphs) {
      assertEquals("长文", paragraph.title());
      assertTrue(paragraph.content().length() <= 2000);
    }
  }

  @Test
  public void absoluteUrlAcceptsOnlyHttpAndDropsFragment() {
    assertEquals("https://example.com/a", WebCrawlService.absoluteUrl("https://example.com/a/#part"));
    assertEquals("https://example.com/a", WebCrawlService.absoluteUrl("https://example.com/a"));
    assertNull(WebCrawlService.absoluteUrl("ftp://example.com/a"));
    assertNull(WebCrawlService.absoluteUrl("javascript:void(0)"));
    assertNull(WebCrawlService.absoluteUrl(""));
    assertEquals("https://example.com/docs", WebCrawlService.directoryPrefix("https://example.com/docs/a.html"));
    assertEquals("https://example.com", WebCrawlService.directoryPrefix("https://example.com/docs"));
  }
}
