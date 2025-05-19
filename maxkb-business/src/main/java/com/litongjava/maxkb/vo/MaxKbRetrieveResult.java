package com.litongjava.maxkb.vo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain=true)
public class MaxKbRetrieveResult {
  private Integer cost;
  private Double run_time;
  private String step_type;
  private String model_name;
  private String problem_text;
  private List<ParagraphSearchResultVo> paragraph_list;
}
