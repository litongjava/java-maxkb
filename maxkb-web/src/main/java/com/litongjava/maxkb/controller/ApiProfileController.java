package com.litongjava.maxkb.controller;

import com.jfinal.kit.Kv;
import com.litongjava.annotation.RequestPath;
import com.litongjava.model.result.ResultVo;

@RequestPath("/api/profile")
public class ApiProfileController {

  @RequestPath
  public ResultVo index() {
    Kv data = Kv.by("version", "v1.5.1 (build at 2024-08-29T17:29, commit: 30b1bdfe)");
    // 将IS_XPACK和XPACK_LICENSE_IS_VALID设置为true,前端将不在提示 创建应用的是的数量限制
    data.set("IS_XPACK", true).set("XPACK_LICENSE_IS_VALID", true);
    return ResultVo.ok(data);
  }
}
