package nexus.io.mosskb.service.kb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.jfinal.kit.Kv;

import dev.langchain4j.data.segment.TextSegment;
import nexus.io.model.upload.UploadResult;

/**
 * 分段标题的生成规则：有标题用标题链，没有标题退回原文件名。
 *
 * <p>用构造好的分段直接验证规则，避免依赖分词器的分块大小；不调用数据库、OCR 或任何外部服务。
 */
public class MossKbDocumentSplitServiceTest {

  private final MossKbDocumentSplitService service = new MossKbDocumentSplitService();

  /** 分段自带的标题优先：每个分段用自己所属的标题链，而不是上一段的标题。 */
  @Test
  public void segmentsUnderHeadingsUseTheirOwnHeadingChain() {
    List<String> titles = service.segmentTitles(List.of(
        segment("# 第一章\n\n第一章的正文。"),
        segment("## 第一节\n\n第一节的正文。"),
        segment("### 小节\n\n小节的正文。"),
        segment("继续属于小节的正文。")), "sample1.pdf");

    assertEquals(List.of("第一章", "第一章 第一节", "第一章 第一节 小节", "第一章 第一节 小节"), titles);
  }

  /** 同级标题替换旧的同名层级，不会一直往后拼接。 */
  @Test
  public void siblingHeadingsReplaceEachOther() {
    List<String> titles = service.segmentTitles(List.of(
        segment("# 甲\n\n甲的正文。"),
        segment("# 乙\n\n乙的正文。")), "sample1.pdf");

    assertEquals(List.of("甲", "乙"), titles);
  }

  /** 整篇没有标题时退回原文件名，避免分段列表出现一整列空标题。 */
  @Test
  public void headingsMissingFallBackToOriginalFileName() {
    List<String> titles = service.segmentTitles(List.of(
        segment("纯文本内容，没有任何 Markdown 标题。"),
        segment("第二段纯文本。")), "扫描件.pdf");

    assertEquals(List.of("扫描件.pdf", "扫描件.pdf"), titles);
  }

  /** 第一个分段没有标题时同样用原文件名兜底，后面的分段不受影响。 */
  @Test
  public void segmentBeforeFirstHeadingFallsBackToFileName() {
    List<String> titles = service.segmentTitles(List.of(
        segment("封面与目录，还没有标题。"),
        segment("# 正文标题\n\n正文。")), "扫描件.pdf");

    assertEquals(List.of("扫描件.pdf", "正文标题"), titles);
  }

  /** 标题过长时按段落标题列的长度截断，不写超长字符串。 */
  @Test
  public void longHeadingIsTruncated() {
    String heading = "标".repeat(400);
    List<String> titles = service.segmentTitles(List.of(segment("# " + heading + "\n\n正文。")), "sample1.pdf");

    assertEquals(1, titles.size());
    assertEquals(255, titles.get(0).length());
    assertTrue(titles.get(0).startsWith("标"));
  }

  /** 真正走一次分词：真实文档切出来的分段标题不能为空。 */
  @Test
  public void everySegmentOfRealDocumentHasTitle() {
    String markdown = "# 河南省自然资源厅办公室文件\n\n> Page 1\n\n"
        + "各省辖市、济源示范区、各省直管县（市）自然资源主管部门：\n\n"
        + "# 一、指导思想\n\n以习近平新时代中国特色社会主义思想为指导，认真贯彻党中央国务院关于全面推进的决策部署。\n";
    List<TextSegment> segments = service.split(markdown);
    assertFalse("分词结果不应为空", segments.isEmpty());

    List<String> titles = service.segmentTitles(segments, "sample1.pdf");

    assertEquals(segments.size(), titles.size());
    for (String title : titles) {
      assertFalse("分段标题不应为空", title.isBlank());
      assertFalse("分段标题不应退回原文件名", "sample1.pdf".equals(title));
    }
    assertTrue("标题应包含文档标题，实际：" + titles, titles.get(0).startsWith("河南省自然资源厅办公室文件"));
  }

  /**
   * 组装的响应里每个分段都要带标题，并且文件名用原文件名。
   *
   * <p>直接调用分段结果组装逻辑，不发起解析、不访问数据库。
   */
  @Test
  @SuppressWarnings("unchecked")
  public void splitResultCarriesTitleAndOriginalFileName() {
    DocumentParsingService.Parsed parsed = new DocumentParsingService.Parsed("pdf-text",
        "# 河南省自然资源厅办公室文件\n\n> Page 1\n\n正文内容。\n", 1);
    UploadResult upload = new UploadResult(12345L, "sample1.pdf", 2048L, "http://example.test/f", "md5");

    List<Kv> results = service.splitParsed(parsed, "sample1.pdf", upload);

    assertEquals(1, results.size());
    Kv file = results.get(0);
    assertEquals("sample1.pdf", file.getStr("name"));
    List<Kv> contents = (List<Kv>) file.get("content");
    assertFalse("分段列表不应为空", contents.isEmpty());
    for (Kv content : contents) {
      assertFalse("分段标题不应为空", content.getStr("title").isBlank());
    }
    assertEquals("河南省自然资源厅办公室文件", contents.get(0).getStr("title"));
  }

  private TextSegment segment(String text) {
    return TextSegment.from(text);
  }
}
