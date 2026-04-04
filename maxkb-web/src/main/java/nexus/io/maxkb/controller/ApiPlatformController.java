package nexus.io.maxkb.controller;

import nexus.io.annotation.RequestPath;
import nexus.io.model.result.ResultVo;

@RequestPath("/api/platform")
public class ApiPlatformController {

  public ResultVo source() {
    return ResultVo.ok();
  }
}
