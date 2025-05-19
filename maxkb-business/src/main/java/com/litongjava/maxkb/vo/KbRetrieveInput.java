package com.litongjava.maxkb.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain=true)
public class KbRetrieveInput {
  private String table;
  private String columns;
  private String input;
  private Float similarity;
  private Integer top_n;
}