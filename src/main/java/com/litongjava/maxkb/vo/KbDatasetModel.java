package com.litongjava.maxkb.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain=true)
public class KbDatasetModel {
  private Long id;
  private String name,desc;
  private Long embedding_mode_id;
  private Integer type;

}
