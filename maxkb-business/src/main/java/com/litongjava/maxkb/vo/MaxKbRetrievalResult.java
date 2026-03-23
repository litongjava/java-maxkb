package com.litongjava.maxkb.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MaxKbRetrievalResult {
  private Long id, dataset_id, document_id;
  private String dataset_name, document_name;
  private String title, question, content;
  private Double similarity, comprehensive_score;
  private String status;
  private boolean is_active;
  private int hit_num;
  private java.sql.Timestamp create_time, update_time;
}
