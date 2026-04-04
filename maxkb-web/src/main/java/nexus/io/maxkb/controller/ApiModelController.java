package nexus.io.maxkb.controller;

import nexus.io.annotation.Delete;
import nexus.io.annotation.EnableCORS;
import nexus.io.annotation.Get;
import nexus.io.annotation.Post;
import nexus.io.annotation.Put;
import nexus.io.annotation.RequestPath;
import nexus.io.jfinal.aop.Aop;
import nexus.io.maxkb.service.kb.MaxKbModelService;
import nexus.io.maxkb.vo.ModelVo;
import nexus.io.model.result.ResultVo;
import nexus.io.tio.boot.http.TioRequestContext;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.utils.json.FastJson2Utils;

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
