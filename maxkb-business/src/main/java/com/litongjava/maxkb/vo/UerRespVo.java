package com.litongjava.maxkb.vo;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UerRespVo {
  private Long id;
  private String username, email, role;
  private List<String> permissions;
  private boolean is_edit_password;
}
