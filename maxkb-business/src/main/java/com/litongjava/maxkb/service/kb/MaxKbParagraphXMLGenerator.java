package com.litongjava.maxkb.service.kb;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.litongjava.maxkb.vo.ParagraphSearchResultVo;

public class MaxKbParagraphXMLGenerator {

  public static String generateXML(List<ParagraphSearchResultVo> records) {
    StringBuilder data = new StringBuilder();
    data.append("<data>");

    // 将记录按 source 列表进行分组
    Map<Long, List<ParagraphSearchResultVo>> groupedBySource = records.stream().collect(Collectors.groupingBy(ParagraphSearchResultVo::getDocument_id));

    int i = 0;
    for (Map.Entry<Long, List<ParagraphSearchResultVo>> entry : groupedBySource.entrySet()) {
      List<ParagraphSearchResultVo> list = entry.getValue();
      data.append("<record>");

      // source
      data.append("<source>").append(i + 1).append("</source>");

      String documentNames = list.get(0).getDocument_name();

      String contents = list.stream().map(ParagraphSearchResultVo::getContent).map(MaxKbParagraphXMLGenerator::escapeXml).collect(Collectors.joining("\r\n"));

      data.append("<document_name>").append(documentNames).append("</document_name>");
      data.append("<contents>").append(contents).append("</contents>");

      data.append("</record>");
      i++;
    }

    data.append("</data>");
    return data.toString();
  }

  // XML 转义方法，确保特殊字符被正确处理
  private static String escapeXml(String input) {
    if (input == null) {
      return "";
    }
    return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
  }
}
