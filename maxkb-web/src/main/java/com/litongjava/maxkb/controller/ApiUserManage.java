package com.litongjava.maxkb.controller;

import com.litongjava.annotation.Get;
import com.litongjava.annotation.RequestPath;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.service.KbUserService;
import com.litongjava.model.result.ResultVo;

@RequestPath("/api/user_manage")
public class ApiUserManage {

  @Get("/{pageNo}/{pageSize}")
  public ResultVo page(Integer pageNo, Integer pageSize) {
    return Aop.get(KbUserService.class).page(pageNo, pageSize);
  }
}
