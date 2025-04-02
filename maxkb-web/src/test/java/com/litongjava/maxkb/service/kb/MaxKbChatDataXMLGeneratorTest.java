package com.litongjava.maxkb.service.kb;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.litongjava.maxkb.vo.ParagraphSearchResultVo;

public class MaxKbChatDataXMLGeneratorTest {

  @Test
  public void test() {
    List<ParagraphSearchResultVo> records = getRecords(); // 假设此方法获取记录
    String xmlData = MaxKbParagraphXMLGenerator.generateXML(records);
    System.out.println(xmlData);
  }

  // 模拟获取记录的方法
  private static List<ParagraphSearchResultVo> getRecords() {
    // 您可以根据实际情况填充数据
    List<ParagraphSearchResultVo> records = new ArrayList<>();

    // 示例数据
    records.add(new ParagraphSearchResultVo(1l, "内容1", "Deloria and Olsen, AMS User's Guide, AMS 129.pdf", 474233893756760066L));
    records.add(new ParagraphSearchResultVo(2L, "内容2", "Deloria and Olsen, AMS User's Guide, AMS 129.pdf", 474233893756760066L));
    records.add(new ParagraphSearchResultVo(2L, "内容3", "Another Document.pdf", 474233893756760070L));

    return records;
  }
}
