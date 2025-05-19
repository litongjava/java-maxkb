package com.litongjava.maxkb.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KbRetrieveInput {
  private String table;
  private String columns;
  private String input;
  private Float similarity;
  private Integer top_n;
}