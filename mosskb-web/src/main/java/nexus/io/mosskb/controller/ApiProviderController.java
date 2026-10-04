package nexus.io.mosskb.controller;

import com.alibaba.fastjson2.JSON;
import nexus.io.annotation.Get;
import nexus.io.annotation.Post;
import nexus.io.annotation.Put;
import nexus.io.annotation.RequestPath;
import nexus.io.jfinal.aop.Aop;
import nexus.io.mosskb.service.kb.ModelCatalogService;
import nexus.io.model.result.ResultVo;
import nexus.io.tio.boot.http.TioRequestContext;
import nexus.io.tio.http.common.HttpRequest;

@RequestPath("/api/provider")
public class ApiProviderController {
  private final ModelCatalogService catalog = Aop.get(ModelCatalogService.class);
  @Get("")
  public ResultVo index(String model_type) { return ResultVo.ok(catalog.providers(model_type)); }
  @Get("/model_type_list")
  public ResultVo model_type_list(String provider) { return ResultVo.ok(catalog.types(provider)); }
  @Get("/model_list")
  public ResultVo model_list(String provider, String model_type) { return ResultVo.ok(catalog.models(provider, model_type)); }
  @Get("/model_form")
  public ResultVo model_form(String provider, String model_type, String model_name) { return ResultVo.ok(catalog.form(provider, "credential_form")); }
  @Get("/model_params_form")
  public ResultVo model_params_form(String provider, String model_type, String model_name) { return ResultVo.ok(catalog.form(provider, "params_form")); }
  @Post("/catalog/discover")
  public ResultVo discover(HttpRequest request) { return catalog.discover(TioRequestContext.getUserIdLong(), JSON.parseObject(request.getBodyString())); }
  @Put("/catalog")
  public ResultVo saveCatalog(HttpRequest request) { return catalog.saveCatalog(TioRequestContext.getUserIdLong(), JSON.parseObject(request.getBodyString())); }
}
