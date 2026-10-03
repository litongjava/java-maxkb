package nexus.io.maxkb.controller;

import nexus.io.annotation.*;
import nexus.io.jfinal.aop.Aop;
import nexus.io.maxkb.service.kb.IsolatedPythonExecutor;
import nexus.io.maxkb.service.kb.PythonFunctionService;
import nexus.io.model.result.ResultVo;
import nexus.io.tio.boot.http.TioRequestContext;
import nexus.io.tio.http.common.HttpRequest;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

@RequestPath("/api/function_lib")
public class ApiFunctionLibController {
  private final PythonFunctionService service = Aop.get(PythonFunctionService.class);
  @Get("")
  public ResultVo all(HttpRequest request) {
    return service.list(TioRequestContext.getUserIdLong(), null, null, request.getParam("name"));
  }
  @Get("/{pageNo}/{pageSize}")
  public ResultVo page(Integer pageNo, Integer pageSize, HttpRequest request) {
    return service.list(TioRequestContext.getUserIdLong(), pageNo, pageSize, request.getParam("name"));
  }
  @Get("/{id}")
  public ResultVo get(Long id) {
    return service.get(TioRequestContext.getUserIdLong(), id);
  }
  @Post("")
  public ResultVo create(HttpRequest request) {
    return service.save(TioRequestContext.getUserIdLong(), null, JSON.parseObject(request.getBodyString()));
  }
  @Put("/{id}")
  public ResultVo update(Long id, HttpRequest request) {
    return service.save(TioRequestContext.getUserIdLong(), id, JSON.parseObject(request.getBodyString()));
  }
  @Delete("/{id}")
  public ResultVo remove(Long id) {
    return service.remove(TioRequestContext.getUserIdLong(), id);
  }
  @Post("/debug")
  public ResultVo debug(HttpRequest request) {
    try {
      return ResultVo.ok(service.debug(JSON.parseObject(request.getBodyString())));
    } catch (RuntimeException e) {
      return ResultVo.fail(e.getMessage());
    }
  }
  @Post("/pylint")
  public ResultVo pylint(HttpRequest request) {
    try {
      JSONObject body = JSON.parseObject(request.getBodyString());
      return ResultVo.ok(Aop.get(IsolatedPythonExecutor.class).execute(body.getString("code"), null, null, true));
    } catch (RuntimeException e) {
      return ResultVo.fail(e.getMessage());
    }
  }
  @Post("/{id}/execute")
  public ResultVo execute(Long id, HttpRequest request) {
    try {
      return ResultVo.ok(service.execute(TioRequestContext.getUserIdLong(), id, JSON.parseObject(request.getBodyString())));
    } catch (RuntimeException e) {
      return ResultVo.fail(e.getMessage());
    }
  }
}
