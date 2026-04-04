package com.litongjava.maxkb.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nexus.io.db.annotation.ATableName;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ATableName("paragraph")
public class KbParagraph {
  private Long id;
  private String title;
  private String question;
  private String content;
  private String status;
  private Integer hitNum;
  private Boolean isActive;
  private Long dataset_id;
  private Long document_id;
  private java.sql.Timestamp create_time;
  private java.sql.Timestamp update_time;
}
