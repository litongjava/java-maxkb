package com.litongjava.maxkb.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain=true)
public class ApiRagDatasetRetrievalRequest {
  private String dataset_name;
  private String input;
  private Double similarity;
  private Integer top_number;
}
