package com.litongjava.maxkb.controller;

import com.litongjava.annotation.Delete;
import com.litongjava.annotation.EnableCORS;
import com.litongjava.annotation.Get;
import com.litongjava.annotation.Post;
import com.litongjava.annotation.Put;
import com.litongjava.annotation.RequestPath;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.service.kb.MaxKbModelService;
import com.litongjava.maxkb.vo.ModelVo;
import com.litongjava.model.result.ResultVo;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.utils.json.FastJson2Utils;

@RequestPath("/api/model")
@EnableCORS
public class ApiModelController {

  MaxKbModelService modelService = Aop.get(MaxKbModelService.class);

  @Get
  public ResultVo index(HttpRequest request) {
    String name = request.getParam("name");
    return modelService.list(name);
  }

  @Post
  public ResultVo save(HttpRequest request) {
    Long userId = TioRequestContext.getUserIdLong();
    String bodyString = request.getBodyString();
    ModelVo modelVo = FastJson2Utils.parse(bodyString, ModelVo.class);
    return modelService.save(userId, modelVo);
  }

  @Put("/{id}")
  public ResultVo update(HttpRequest request,Long id) {
    Long userId = TioRequestContext.getUserIdLong();
    String bodyString = request.getBodyString();
    ModelVo modelVo = FastJson2Utils.parse(bodyString, ModelVo.class);
    modelVo.setId(id);
    return modelService.save(userId, modelVo);
  }

  @Delete("/{id}")
  public ResultVo delete(Long id) {
    return modelService.delete(id);
  }

  @Get("/{id}")
  public ResultVo get(Long id) {
    return modelService.get(id);
  }

}
