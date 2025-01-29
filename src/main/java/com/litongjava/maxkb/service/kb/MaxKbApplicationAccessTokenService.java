package com.litongjava.maxkb.service.kb;

import java.util.List;

import com.jfinal.kit.Kv;
import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.constant.AppConstant;
import com.litongjava.maxkb.model.MaxKbApplicationAccessToken;
import com.litongjava.maxkb.model.MaxKbApplicationPublicAccessClient;
import com.litongjava.maxkb.service.AuthService;
import com.litongjava.model.result.ResultVo;
import com.litongjava.tio.utils.json.JsonUtils;
import com.litongjava.tio.utils.jwt.JwtUtils;
import com.litongjava.tio.utils.mcid.McIdUtils;
import com.litongjava.tio.utils.snowflake.SnowflakeIdUtils;
import com.litongjava.tio.utils.token.TokenManager;

public class MaxKbApplicationAccessTokenService {

  public void list() {
    List<MaxKbApplicationAccessToken> find = new MaxKbApplicationAccessToken().find();
    System.out.println(JsonUtils.toJson(find));
  }

  public void create(Long applicationId) {
    Long id = McIdUtils.id();
    MaxKbApplicationAccessToken token = new MaxKbApplicationAccessToken().setApplicationId(applicationId)
        //
        .setAccessToken(id).setIsActive(true).setAccessNum(100).setWhiteActive(false)
        //
        .setWhiteList(new String[] {})
        //
        .setShowSource(false);
    token.save();
  }

  public void delete(Long applicationId) {
    MaxKbApplicationAccessToken token = new MaxKbApplicationAccessToken().setApplicationId(applicationId);
    token.delete();
  }

  public ResultVo getById(Long applicationId) {
    Row record = Db.findById(MaxKbApplicationAccessToken.tableName, MaxKbApplicationAccessToken.primaryKey, applicationId);
    if (record != null) {
      return ResultVo.ok(record.toKv());
    } else {
      return ResultVo.ok(Kv.create());
    }

  }

  public ResultVo authentication(Long shortToken, String longToken) {
    String sql = "select application_id from %s where access_token=? and deleted=0";
    sql = String.format(sql, MaxKbApplicationAccessToken.tableName);
    Long applicationId = Db.queryLong(sql, shortToken);
    if (applicationId == null) {
      return ResultVo.fail("not found applicaiton id:" + shortToken);
    }

    Long clientId = Aop.get(AuthService.class).getIdByToken(longToken);
    if (clientId != null) {
      sql = "select count(1) from $table_name where application_id=? and client_id=?";
      boolean exist = MaxKbApplicationPublicAccessClient.dao.existsBySql(sql, applicationId, clientId);
      if (exist) {
        return ResultVo.ok(longToken);
      }
    } else {
      clientId = SnowflakeIdUtils.id();
    }
    Long id = SnowflakeIdUtils.id();
    longToken = JwtUtils.createTokenByUserId(AppConstant.SECRET_KEY, clientId);
    TokenManager.login(clientId, longToken);
    new MaxKbApplicationPublicAccessClient().setId(id).setClientId(clientId).setApplicationId(applicationId)
        //
        .setAccessNum(0).setIntradayAccessNum(0)
        //
        .save();
    return ResultVo.ok(longToken);

  }

}
