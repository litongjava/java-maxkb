package com.litongjava.maxkb.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ParagraphSearchResultVo {
  private Long id;
  private String content, title;
  private Long dataset_id;
  private String dataset_name;
  private Long document_id;
  private String document_name;
  private Boolean active;
  private Integer hit_num;
  private String status;
}
