package com.litongjava.maxkb.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.jfinal.kit.Kv;
import com.litongjava.annotation.RequestPath;
import com.litongjava.maxkb.enumeration.ModelProvider;
import com.litongjava.model.result.ResultVo;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.utils.hutool.FileUtil;
import com.litongjava.tio.utils.hutool.ResourceUtil;

import lombok.extern.slf4j.Slf4j;

@RequestPath("/api/provider")
@Slf4j
public class ApiProviderController {

  @RequestPath
  public HttpResponse index() {
    URL resource = ResourceUtil.getResource("json/api_provider.json");
    StringBuilder jsonString = FileUtil.readURLAsString(resource);
    HttpResponse response = TioRequestContext.getResponse();
    response.setJson(jsonString.toString());
    return response;
  }

  @RequestPath("/model_type_list")
  public ResultVo model_type_list() {
    List<Kv> kvs = new ArrayList<>();
    kvs.add(Kv.by("key", "大语言模型").set("value", "LLM"));
    kvs.add(Kv.by("key", "向量模型").set("value", "EMBEDDING"));
    return ResultVo.ok("成功", kvs);
  }

  @RequestPath("/model_list")
  public HttpResponse model_list(String provider, String model_type) {
    model_type = model_type.toLowerCase();
    String filename = provider + "_" + model_type + ".json";

    log.info("filename:{}",filename);
    URL resource = ResourceUtil.getResource("json/" + filename);
    StringBuilder jsonString = FileUtil.readURLAsString(resource);

    HttpResponse response = TioRequestContext.getResponse();
    response.setJson(jsonString.toString());
    return response;
  }

  public HttpResponse model_form(String provider, String model_type, String model_name) {

    String filename =null;
    if(ModelProvider.model_openai_provider.getName().equals(provider)) {
      filename = provider + "_model_form.json";
    }
    
    log.info("filename:{}",filename);
    URL resource = ResourceUtil.getResource("json/" + filename);
    StringBuilder jsonString = FileUtil.readURLAsString(resource);
    HttpResponse response = TioRequestContext.getResponse();
    response.setJson(jsonString.toString());
    return response;
  }
}
