package com.litongjava.maxkb.controller;

import com.jfinal.kit.Kv;
import com.litongjava.annotation.RequestPath;
import com.litongjava.model.result.ResultVo;

@RequestPath("/api/profile")
public class ApiProfileController {

  @RequestPath
  public ResultVo index() {
    Kv data = Kv.by("version", "v1.5.1 (build at 2024-08-29T17:29, commit: 30b1bdfe)");
    data.set("IS_XPACK", false).set("XPACK_LICENSE_IS_VALID", false);
    return ResultVo.ok(data);
  }
}
