package com.litongjava.maxkb.vo;

import java.util.List;

import com.litongjava.maxkb.model.MaxKbParagraphId;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProbrolemCreateBatch {
  private List<Long> problem_id_list;
  private List<MaxKbParagraphId> paragraph_list;
}
