package com.litongjava.maxkb.utils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class PdfUtils {

  /**
   * 从PDF字节数据中提取文本内容
   * @param pdfBytes PDF文件的字节数组
   * @return 提取的文本内容
   * @throws IOException 如果PDF解析失败
   */
  public static String parseContent(byte[] pdfBytes) throws IOException {
    try (InputStream is = new ByteArrayInputStream(pdfBytes); PDDocument document = PDDocument.load(is)) {
      PDFTextStripper stripper = new PDFTextStripper();
      stripper.setSortByPosition(true); // 按页面布局排序
      stripper.setAddMoreFormatting(true); // 保留更多格式信息
      return stripper.getText(document).replace("\u0000", "");
    }
  }
}