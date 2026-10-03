package nexus.io.maxkb.controller;

import nexus.io.annotation.Get;
import nexus.io.annotation.Post;
import nexus.io.annotation.Put;
import nexus.io.annotation.Delete;
import nexus.io.maxkb.service.UserManagementService;
import nexus.io.tio.http.common.HttpRequest;
import com.alibaba.fastjson2.JSON;
import nexus.io.annotation.RequestPath;
import nexus.io.jfinal.aop.Aop;
import nexus.io.maxkb.service.KbUserService;
import nexus.io.model.result.ResultVo;

@RequestPath("/api/user_manage")
public class ApiUserManage {

  @Post("")
  public ResultVo create(HttpRequest request) {
    return Aop.get(UserManagementService.class).create(JSON.parseObject(request.getBodyString()));
  }

  @Put("/{id}")
  public ResultVo update(Long id, HttpRequest request) {
    return Aop.get(UserManagementService.class).update(id, JSON.parseObject(request.getBodyString()));
  }

  @Get("/{pageNo}/{pageSize}")
  public ResultVo page(Integer pageNo, Integer pageSize, String email_or_username) {
    return Aop.get(KbUserService.class).page(pageNo, pageSize, email_or_username);
  }

  @Put("/{id}/re_password")
  public ResultVo resetPassword(Long id, HttpRequest request) {
    return Aop.get(UserManagementService.class).resetPassword(id, JSON.parseObject(request.getBodyString()));
  }

  @Delete("/{id}")
  public ResultVo delete(Long id) {
    return Aop.get(UserManagementService.class).delete(id);
  }
}
