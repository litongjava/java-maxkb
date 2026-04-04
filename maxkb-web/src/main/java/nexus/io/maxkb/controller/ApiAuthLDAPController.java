package nexus.io.maxkb.controller;

import nexus.io.annotation.RequestPath;
import nexus.io.model.result.ResultVo;

@RequestPath("/api/auth/LDAP")
public class ApiAuthLDAPController {

  public ResultVo detail() {
    return ResultVo.ok();
  }
}
