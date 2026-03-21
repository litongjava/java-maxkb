package com.litongjava.maxkb.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiRagDatasetRetrievalRequest {
  private String datasetName;
  private String input;
}
