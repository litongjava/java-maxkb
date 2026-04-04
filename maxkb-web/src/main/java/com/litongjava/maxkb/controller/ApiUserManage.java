package com.litongjava.maxkb.controller;

import com.litongjava.maxkb.service.KbUserService;

import nexus.io.annotation.Get;
import nexus.io.annotation.RequestPath;
import nexus.io.jfinal.aop.Aop;
import nexus.io.model.result.ResultVo;

@RequestPath("/api/user_manage")
public class ApiUserManage {

  @Get("/{pageNo}/{pageSize}")
  public ResultVo page(Integer pageNo, Integer pageSize) {
    return Aop.get(KbUserService.class).page(pageNo, pageSize);
  }
}
