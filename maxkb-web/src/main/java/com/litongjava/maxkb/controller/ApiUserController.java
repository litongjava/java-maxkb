package com.litongjava.maxkb.controller;

import java.util.ArrayList;
import java.util.List;

import com.jfinal.kit.Kv;
import com.litongjava.annotation.Get;
import com.litongjava.annotation.RequestPath;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.service.UserService;
import com.litongjava.maxkb.vo.UserLoginReqVo;
import com.litongjava.model.result.ResultVo;
import com.litongjava.tio.boot.http.TioRequestContext;

@RequestPath("/api/user")
public class ApiUserController {

  @RequestPath
  public ResultVo index() {
    Long id = TioRequestContext.getUserIdLong();
    return Aop.get(UserService.class).index(id);
  }

  public ResultVo login(UserLoginReqVo vo) {
    return Aop.get(UserService.class).login(vo);
  }

  public ResultVo logout() {
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(UserService.class).logout(userId);
  }

  @Get("/list/APPLICATION")
  public ResultVo listApplication() {
    List<Kv> kvs = new ArrayList<>();
//    kvs.add(Kv.by("username", "all").set("id", "1"));
//    kvs.add(Kv.by("username", "type").set("id", "2"));
    return ResultVo.ok(kvs);
  }

  @Get("/list/DATASET")
  public ResultVo listDATASET() {
    List<Kv> kvs = new ArrayList<>();
    return ResultVo.ok(kvs);
  }

  @Get("/list/FUNCTION")
  public ResultVo listFUNCTION() {
    List<Kv> kvs = new ArrayList<>();
    return ResultVo.ok(kvs);
  }
}
