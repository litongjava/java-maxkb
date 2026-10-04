package nexus.io.mosskb.controller;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson2.JSON;
import com.jfinal.kit.Kv;

import nexus.io.annotation.Get;
import nexus.io.annotation.Post;
import nexus.io.annotation.RequestPath;
import nexus.io.jfinal.aop.Aop;
import nexus.io.mosskb.service.KbUserService;
import nexus.io.mosskb.vo.UserLoginReqVo;
import nexus.io.model.result.ResultVo;
import nexus.io.tio.boot.http.TioRequestContext;
import nexus.io.tio.http.common.HttpRequest;

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

  /**
   * 切换当前登录用户的界面语言。前端头像菜单的语言下拉调用 POST /api/user/language，
   * 身份由登录令牌确定，请求体只需要 language。
   */
  @Post("/language")
  public ResultVo language(HttpRequest request) {
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(KbUserService.class).switchLanguage(userId, JSON.parseObject(request.getBodyString()));
  }

  /**
   * 修改当前登录用户的密码。身份由登录令牌确定，不再校验邮箱验证码，请求体只需要新密码和确认密码。
   */
  @Post("/current/reset_password")
  public ResultVo resetCurrentPassword(HttpRequest request) {
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(KbUserService.class).resetCurrentPassword(userId, JSON.parseObject(request.getBodyString()));
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
