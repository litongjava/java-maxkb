package com.litongjava.maxkb.service.kb;

import java.util.ArrayList;
import java.util.List;

import com.jfinal.kit.Kv;
import com.litongjava.db.TableInput;
import com.litongjava.db.TableResult;
import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.kit.PgObjectUtils;
import com.litongjava.maxkb.constant.TableNames;
import com.litongjava.maxkb.dao.MaxKbApplicationDao;
import com.litongjava.maxkb.model.MaxKbApplication;
import com.litongjava.maxkb.model.MaxKbApplicationDatasetMapping;
import com.litongjava.maxkb.model.MaxKbApplicationPublicAccessClient;
import com.litongjava.maxkb.model.MaxKbModel;
import com.litongjava.maxkb.vo.MaxKbApplicationVo;
import com.litongjava.maxkb.vo.MaxKbDatasetSettingVo;
import com.litongjava.maxkb.vo.MaxKbModelParamsSetting;
import com.litongjava.maxkb.vo.MaxKbModelSetting;
import com.litongjava.model.page.Page;
import com.litongjava.model.result.ResultVo;
import com.litongjava.table.services.ApiTable;
import com.litongjava.tio.utils.snowflake.SnowflakeIdUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MaxKbApplicationService {

  public ResultVo create(Long userId, MaxKbApplicationVo application) {
    Long applicationId = SnowflakeIdUtils.id();
    application.setId(applicationId);
    Row record = Row.fromBean(application);
    record.remove("dataset_id_list");
    record.set("user_id", userId);
    Db.save(MaxKbApplication.tableName, record);

    Aop.get(MaxKbApplicationAccessTokenService.class).create(applicationId);
    return ResultVo.ok(application);
  }

  public ResultVo update(Long userId, MaxKbApplicationVo application) {
    List<Long> dataset_id_list = application.getDataset_id_list();
    List<MaxKbApplicationDatasetMapping> saveRecords = new ArrayList<>();
    if (dataset_id_list != null) {
      for (Long datasetId : dataset_id_list) {
        MaxKbApplicationDatasetMapping mapping = new MaxKbApplicationDatasetMapping();
        mapping.setId(SnowflakeIdUtils.id()).setDatasetId(datasetId).setApplicationId(application.getId());
        saveRecords.add(mapping);
      }
    }

    Row record = Row.fromBean(application);
    record.remove("dataset_id_list");
    record.set("user_id", userId);

    Db.tx(() -> {
      Db.deleteById(MaxKbApplicationDatasetMapping.tableName, "application_id", application.getId());
      if (saveRecords.size() > 0) {
        Db.batchSave(saveRecords, 2000);
      }

      Db.update(MaxKbApplication.tableName, record);
      return true;
    });

    return ResultVo.ok();
  }

  public ResultVo delete(Long userId, Long applicationId) {
    boolean deleted = false;
    if (userId != null && userId.equals(1L)) {
      deleted = new MaxKbApplication().setId(applicationId).delete();
    } else {
      deleted = new MaxKbApplication().setId(applicationId).setUserId(userId).delete();
    }
    new MaxKbApplicationDatasetMapping().setApplicationId(applicationId).delete();
    Aop.get(MaxKbApplicationAccessTokenService.class).delete(applicationId);
    return ResultVo.ok(deleted);
  }

  public ResultVo page(TableInput tableInput) {
    Integer pageNo = tableInput.getPageNo();
    Integer pageSize = tableInput.getPageSize();
    log.info("page:{},{}", pageNo, pageSize);
    tableInput.setFrom(TableNames.max_kb_application);
    TableResult<Page<Row>> result = ApiTable.page(tableInput);
    Page<Row> page = result.getData();
    List<Row> records = page.getList();
    List<Kv> kvs = new ArrayList<>();
    for (Row record : records) {
      PgObjectUtils.toBean(record, "model_setting", MaxKbModelSetting.class);
      PgObjectUtils.toBean(record, "model_params_setting", MaxKbModelParamsSetting.class);
      PgObjectUtils.toBean(record, "dataset_setting", MaxKbDatasetSettingVo.class);
      kvs.add(record.toKv());
    }
    Kv kv = Kv.by("current", pageNo).set("size", pageSize).set("total", page.getTotalRow()).set("records", kvs);
    return ResultVo.ok(kv);
  }

  public ResultVo list(Long userId) {
    Row quereyRecord = new Row();
    if (userId.equals(1L)) {

    } else {
      quereyRecord.set("user_id", userId);
    }

    List<Row> records = Db.find(TableNames.max_kb_application, quereyRecord);
    List<Kv> kvs = new ArrayList<>();
    for (Row record : records) {
      PgObjectUtils.toBean(record, "model_params_setting", MaxKbModelParamsSetting.class);
      PgObjectUtils.toBean(record, "model_setting", MaxKbModelSetting.class);
      PgObjectUtils.toBean(record, "dataset_setting", MaxKbDatasetSettingVo.class);
      Kv kv = record.toKv();
      kvs.add(kv);
    }
    return ResultVo.ok(kvs);
  }

  public ResultVo get(Long userId, Long applicationId) {

    log.info("user_id:{}", userId);
    Row quereyRecord = null;
    if (userId.equals(1L)) {
      quereyRecord = Row.by("id", applicationId);
    } else {
      quereyRecord = Row.by("id", applicationId).set("user_id", userId);
    }

    Row record = Db.findFirst(MaxKbApplication.tableName, quereyRecord);
    PgObjectUtils.toBean(record, "model_params_setting", MaxKbModelParamsSetting.class);
    PgObjectUtils.toBean(record, "dataset_setting", MaxKbDatasetSettingVo.class);

    Object object = record.get("model_setting");
    if (object != null) {
      PgObjectUtils.toBean(record, "model_setting", MaxKbModelSetting.class);
    } else {
      MaxKbModelSetting maxKbModelSetting = new MaxKbModelSetting();
      maxKbModelSetting.setSystem("你是 xxx 小助手").setPrompt("已知信息：{data}\n用户问题：{question}\n回答要求：\n - 请使用中文回答用户问题")
          //
          .setNo_references_prompt("{question}");

      record.set("model_setting", maxKbModelSetting);
    }

    Long modelId = record.getLong("model_id");
    record.remove("model_id");

    String sql = String.format("select dataset_id from %s where application_id=?", MaxKbApplicationDatasetMapping.tableName);
    List<Long> dataset_id_list = Db.queryListLong(sql, applicationId);
    record.set("dataset_id_list", dataset_id_list);
    record.set("model", modelId);
    Kv kv = record.toKv();
    return ResultVo.ok(kv);
  }

  public ResultVo listApplicaionModel(Long userId, Long applicationId) {
    Row queryRecord = Row.by("user_id", userId);
    String columns = "id,name,provider,model_type,model_name,status,meta,permission_type,user_id";
    List<Row> list = Db.find(MaxKbModel.tableName, columns, queryRecord);
    List<Kv> kvs = new ArrayList<>();
    MaxKbUserService maxKbUserService = Aop.get(MaxKbUserService.class);

    for (Row record : list) {
      Long user_id = record.getLong("user_id");
      String username = maxKbUserService.queryUsername(user_id);
      record.set("username", username);
      kvs.add(record.toKv());
    }

    return ResultVo.ok(kvs);
  }

  public ResultVo setModelId(Long userIdLong, Long applicationId, Long modelId) {
    boolean update = false;
    if (userIdLong.equals(1L)) {
      Row updateRecord = Row.by("id", modelId).set("id", applicationId).set("model_id", modelId);
      update = Db.update(MaxKbApplication.tableName, "id", updateRecord);
    } else {
      Row updateRecord = Row.by("id", modelId).set("id", applicationId).set("user_id", userIdLong).set("model_id", modelId);
      update = Db.update(MaxKbApplication.tableName, "id,user_id", updateRecord);
    }

    return ResultVo.ok(update);
  }

  public ResultVo listApplicaionDataset(Long userId, Long applicationId) {
    return Aop.get(MaxKbDatasetService.class).list(userId);
  }

  public ResultVo profile(Long clientId) {
    Long applicationId = Db.queryLongById(MaxKbApplicationPublicAccessClient.tableName, "client_id", clientId);
    if (applicationId == null) {
      return ResultVo.fail("applicationId is null");
    }
    Row applicaiton = Aop.get(MaxKbApplicationDao.class).getBasicInfoById(applicationId);
    if (applicaiton == null) {
      return ResultVo.ok();
    }

    return ResultVo.ok(applicaiton.toMap());

  }
}
