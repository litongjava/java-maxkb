package com.litongjava.maxkb.dao;

import java.util.Map;

import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.maxkb.constant.TableNames;
import com.litongjava.maxkb.vo.ModelVo;
import com.litongjava.tio.utils.snowflake.SnowflakeIdUtils;

public class ModelDao {

  public void save(Map<String, Object> map) {
    Db.save(TableNames.max_kb_model, Row.fromMap(map));
  }

  public boolean deleteById(Long id) {
    return Db.deleteById(TableNames.max_kb_model, id);
  }

  public boolean saveOrUpdate(Long userId, ModelVo modelVo) {
    Row record = new Row();
    record.set("name", modelVo.getName()).set("model_type", modelVo.getModel_type()).set("model_name", modelVo.getModel_name())
        //
        .set("permission_type", modelVo.getPermission_type())
        //
        .set("credential", modelVo.getCredential()).set("user_id", userId).set("status", "SUCCESS");

    String provider = modelVo.getProvider();
    if (provider != null) {
      record.set("provider", provider);
    }

    Long id = modelVo.getId();
    if (id != null) {
      record.set("id", id);
      return Db.update(TableNames.max_kb_model, "id", record, new String[] { "credential" });
    } else {
      record.set("id", SnowflakeIdUtils.id());
      return Db.save(TableNames.max_kb_model, record, new String[] { "credential" });
    }
  }

}
