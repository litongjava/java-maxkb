package nexus.io.maxkb.controller;

import nexus.io.annotation.RequestPath;
import nexus.io.model.result.ResultVo;

@RequestPath("/api/team")
public class ApiTeamController {

  @RequestPath("/member")
  public ResultVo member() {
    return ResultVo.ok();
  }
}
