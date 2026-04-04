package nexus.io.maxkb.controller;

import nexus.io.annotation.Get;
import nexus.io.annotation.RequestPath;
import nexus.io.model.result.ResultVo;

@RequestPath("/api/qr_type")
public class ApiQrTypeController {

  @Get
  public ResultVo index() {
    //{ "code": 200, "data": ["wecom", "dingtalk"] }
    String[] data = { "wecom", "dingtalk" };
    return ResultVo.ok(data);
  }
}
