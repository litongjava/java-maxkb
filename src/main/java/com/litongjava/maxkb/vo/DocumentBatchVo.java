package com.litongjava.maxkb.vo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class DocumentBatchVo {
  private Long id;
  private String name;
  private List<Paragraph> paragraphs;
}
