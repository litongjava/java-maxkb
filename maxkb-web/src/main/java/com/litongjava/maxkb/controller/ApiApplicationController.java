package com.litongjava.maxkb.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSONObject;
import com.litongjava.maxkb.service.kb.MaxKbApplicationAccessTokenService;
import com.litongjava.maxkb.service.kb.MaxKbApplicationCharRecordService;
import com.litongjava.maxkb.service.kb.MaxKbApplicationHitTestService;
import com.litongjava.maxkb.service.kb.MaxKbApplicationService;
import com.litongjava.maxkb.vo.MaxKbApplicationVo;
import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.utils.json.FastJson2Utils;
import com.litongjava.tio.utils.json.JsonUtils;

import nexus.io.annotation.Delete;
import nexus.io.annotation.Get;
import nexus.io.annotation.Post;
import nexus.io.annotation.Put;
import nexus.io.annotation.RequestPath;
import nexus.io.db.TableInput;
import nexus.io.jfinal.aop.Aop;
import nexus.io.model.result.ResultVo;
import nexus.io.table.constants.Operators;
import nexus.io.tio.boot.http.TioRequestContext;
import nexus.io.tio.http.common.HttpRequest;

@RequestPath("/api/application")
public class ApiApplicationController {

  @Post
  public ResultVo create(MaxKbApplicationVo application) {
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(MaxKbApplicationService.class).create(userId, application);
  }

  @Put("/{applicationId}")
  public ResultVo update(Long applicationId, HttpRequest request) {

    String bodyString = request.getBodyString();
    MaxKbApplicationVo application = JsonUtils.parse(bodyString, MaxKbApplicationVo.class);

    Long userId = TioRequestContext.getUserIdLong();
    application.setId(applicationId);
    return Aop.get(MaxKbApplicationService.class).update(userId, application);
  }

  @Delete("/{applicationId}")
  public ResultVo delete(Long applicationId) {
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(MaxKbApplicationService.class).delete(userId, applicationId);
  }

  @Get("/{applicationId}")
  public ResultVo getById(Long applicationId) {
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(MaxKbApplicationService.class).get(userId, applicationId);
  }

  @Get
  public ResultVo list() {
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(MaxKbApplicationService.class).list(userId);
  }

  @Get("/{pageNo}/{pageSize}")
  public ResultVo page(Integer pageNo, Integer pageSize, String name) {
    TableInput tableInput = new TableInput();
    tableInput.setPageNo(pageNo).setPageSize(pageSize);
    //tableInput.setSearchKey(name);
    tableInput.set("name", name);
    tableInput.set("name_op", Operators.CT);
    tableInput.orderBy("name");
    return Aop.get(MaxKbApplicationService.class).page(tableInput);
  }

  @Get("/{applicationId}/access_token")
  public ResultVo getAccessToken(Long applicationId) {
    return Aop.get(MaxKbApplicationAccessTokenService.class).getById(applicationId);
  }

  @Get("/{applicationId}/statistics/chat_record_aggregate_trend")
  public ResultVo statisticsOfCharRecordAggreagteTrend(Long applicationId, String start_time, String end_time) {
    // Parse start and end dates
    LocalDate startDate = LocalDate.parse(start_time);
    LocalDate endDate = LocalDate.parse(end_time);

    // Initialize list to hold data for each day
    List<Map<String, Object>> dataList = new ArrayList<>();

    // Loop through the date range
    LocalDate currentDate = startDate;
    while (!currentDate.isAfter(endDate)) {
      // Fetch or compute statistics for each day
      Map<String, Object> dailyData = new HashMap<>();
      dailyData.put("star_num", 0); // Replace with actual data retrieval
      dailyData.put("trample_num", 0);
      dailyData.put("tokens_num", 0);
      dailyData.put("chat_record_count", 0);
      dailyData.put("customer_num", 0);
      dailyData.put("day", currentDate.toString());
      dailyData.put("customer_added_count", 0);

      dataList.add(dailyData);
      // Move to the next day
      currentDate = currentDate.plusDays(1);
    }
    return ResultVo.ok(dataList);
  }

  @Get("/{applicationId}/model")
  public ResultVo listApplicaionModel(Long applicationId) {
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(MaxKbApplicationService.class).listApplicaionModel(userId, applicationId);
  }

  @Get("/{applicationId}/model_params_form/{modelId}")
  public ResultVo setModelId(Long applicationId, Long modelId) {
    Long userIdLong = TioRequestContext.getUserIdLong();
    return Aop.get(MaxKbApplicationService.class).setModelId(userIdLong, applicationId, modelId);
  }

  @Get("/{applicationId}/list_dataset")
  public ResultVo listApplicaionDataset(Long applicationId) {
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(MaxKbApplicationService.class).listApplicaionDataset(userId, applicationId);
  }

  @Get("/{applicationId}/chat/{chatId}/chat_record/{recordId}")
  public ResultVo getRecord(Long applicationId, Long chatId, Long recordId) {
    Long userId = TioRequestContext.getUserIdLong();
    return Aop.get(MaxKbApplicationCharRecordService.class).get(userId, applicationId, chatId, recordId);
  }

  @Get("/{applicationId}/hit_test")
  public ResultVo hitTest(Long applicationId, HttpRequest request) {
    Long userId = TioRequestContext.getUserIdLong();

    String query_text = request.getParam("query_text");
    Double similarity = request.getDouble("similarity");
    Integer top_number = request.getInt("top_number");
    String search_mode = request.getParam("search_mode");
    return Aop.get(MaxKbApplicationHitTestService.class).hitTest(userId, applicationId, query_text, similarity, top_number, search_mode);
  }

  @Post("/authentication")
  public ResultVo authentication(HttpRequest request) {
    String authorization = request.getAuthorization();
    String bodyString = request.getBodyString();
    if (StrUtil.isBlank(bodyString)) {
      return ResultVo.fail();
    }

    JSONObject parseObject = FastJson2Utils.parseObject(bodyString);
    Long access_token = parseObject.getLong("access_token");
    return Aop.get(MaxKbApplicationAccessTokenService.class).authentication(access_token, authorization);
  }

  @Get("/profile")
  public ResultVo profile(HttpRequest httpRequest) {
    String referer = httpRequest.getReferer();
    
    Long clientId = TioRequestContext.getUserIdLong();
    return Aop.get(MaxKbApplicationService.class).profile(clientId);
  }

}
