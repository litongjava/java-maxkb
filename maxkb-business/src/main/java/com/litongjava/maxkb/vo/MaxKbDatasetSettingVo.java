package com.litongjava.maxkb.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain=true)
public class MaxKbDatasetSettingVo {
  private Integer top_n;
  private Float similarity;
  private Integer max_paragraph_char_number;
  private String search_mode;
  private MaxKbApplicationNoReferencesSetting no_references_setting;
}
