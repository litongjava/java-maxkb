package com.litongjava.maxkb.vo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@AllArgsConstructor
public class ResultPage<T> {
  private int current;
  private int size;
  private int total;
  private List<T> records;
}
