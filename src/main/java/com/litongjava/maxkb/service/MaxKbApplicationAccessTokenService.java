package com.litongjava.maxkb.service;

import java.util.List;

import com.jfinal.kit.Kv;
import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Record;
import com.litongjava.maxkb.model.MaxKbApplicationAccessToken;
import com.litongjava.model.result.ResultVo;
import com.litongjava.tio.utils.json.JsonUtils;
import com.litongjava.tio.utils.mcid.McIdUtils;

public class MaxKbApplicationAccessTokenService {

  public void list() {
    List<MaxKbApplicationAccessToken> find = new MaxKbApplicationAccessToken().find();
    System.out.println(JsonUtils.toJson(find));
  }

  public void create(Long applicationId) {
    Long id = McIdUtils.id();
    MaxKbApplicationAccessToken token = new MaxKbApplicationAccessToken().setApplicationId(applicationId)
        //
        .setAccessToken(id.toString()).setIsActive(true).setAccessNum(100).setWhiteActive(false)
        //
        .setWhiteList(new String[] {})
        //
        .setShowSource(false);
    token.save();
  }

  public void delete(Long userId, Long applicationId) {
    MaxKbApplicationAccessToken token = new MaxKbApplicationAccessToken().setApplicationId(applicationId);
    token.delete();
  }

  public ResultVo getById(Long applicationId) {
    Record record = Db.findById(MaxKbApplicationAccessToken.tableName, MaxKbApplicationAccessToken.primaryKey, applicationId);
    if(record!=null) {
      return ResultVo.ok(record.toKv());
    }else {
      return ResultVo.ok(Kv.create());
    }
    
  }
}
