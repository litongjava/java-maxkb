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
  private String document_type;
  private String document_url;
  private Long paragraph_id;
  
  private Boolean active;
  private Integer hit_num;
  private String status;

  public ParagraphSearchResultVo(long id, String content, String document_name, long document_id) {
    this.id = id;
    this.content = content;
    this.document_name = document_name;
    this.document_id = document_id;
  }
}
