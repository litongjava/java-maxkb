package com.litongjava.maxkb.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaxKbParagraphId {
  private Long paragraph_id;
  private Long document_id;
}