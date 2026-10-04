package nexus.io.mosskb.controller;

import nexus.io.annotation.Delete;
import nexus.io.annotation.EnableCORS;
import nexus.io.annotation.Get;
import nexus.io.annotation.Post;
import nexus.io.annotation.Put;
import nexus.io.annotation.RequestPath;
import nexus.io.jfinal.aop.Aop;
import nexus.io.mosskb.service.kb.MossKbModelService;
import nexus.io.mosskb.vo.ModelVo;
import nexus.io.model.result.ResultVo;
import nexus.io.tio.boot.http.TioRequestContext;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.utils.json.FastJson2Utils;

@RequestPath("/api/model")
@EnableCORS
public class ApiModelController {

  MossKbModelService modelService = Aop.get(MossKbModelService.class);

  @Get("")
  public ResultVo index(HttpRequest request) {
    String name = request.getParam("name");
    return modelService.list(name, request.getParam("model_type"));
  }

  @Post("")
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

  @Get("/{id}/model_params_form")
  public ResultVo modelParams(Long id) {
    nexus.io.db.activerecord.Row row = nexus.io.db.activerecord.Db.findById("moss_kb_model", id);
    if (row == null) {
      return ResultVo.fail(404, "模型不存在");
    }
    return ResultVo.ok(com.alibaba.fastjson2.JSON.parseArray(row.getStr("model_params_form")));
  }

  @Put("/{id}/model_params_form")
  public ResultVo updateModelParams(Long id, HttpRequest request) {
    com.alibaba.fastjson2.JSONArray form = com.alibaba.fastjson2.JSON.parseArray(request.getBodyString());
    nexus.io.db.activerecord.Db.update("update moss_kb_model set model_params_form=?::jsonb where id=?", form.toJSONString(), id);
    return ResultVo.ok(form);
  }

}
