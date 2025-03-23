package com.litongjava.maxkb.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//{"username":"admin","password":"Kimi@2024"}
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginReqVo {
  private String username, password;
}
