package nexus.io.maxkb.controller;

import java.util.ArrayList;
import java.util.List;

import com.jfinal.kit.Kv;

import nexus.io.annotation.Get;
import nexus.io.annotation.RequestPath;
import nexus.io.jfinal.aop.Aop;
import nexus.io.maxkb.service.KbUserService;
import nexus.io.maxkb.vo.UserLoginReqVo;
import nexus.io.model.result.ResultVo;
import nexus.io.tio.boot.http.TioRequestContext;

@RequestPath("/api/user")
public class ApiUserController {

  @RequestPath
  public ResultVo index() {
    Long id = TioRequestContext.getUserIdLong();
    return Aop.get(KbUserService.class).index(id);
  }

  public ResultVo login(UserLoginReqVo vo) {
    return Aop.get(KbUserService.class).login(vo);
  }

  public ResultVo logout() {
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(KbUserService.class).logout(userId);
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
