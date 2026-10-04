package nexus.io.mosskb.dao;

import java.util.Map;

import nexus.io.db.activerecord.Db;
import nexus.io.db.activerecord.Row;
import nexus.io.mosskb.constant.MossKbTableNames;
import nexus.io.mosskb.vo.ModelVo;
import nexus.io.tio.utils.snowflake.SnowflakeIdUtils;

public class ModelDao {

  public void save(Map<String, Object> map) {
    Db.save(MossKbTableNames.moss_kb_model, Row.fromMap(map));
  }

  public boolean deleteById(Long id) {
    return Db.deleteById(MossKbTableNames.moss_kb_model, id);
  }

  public boolean saveOrUpdate(Long userId, ModelVo modelVo) {
    String provider = modelVo.getProvider();
    // 模型必须挂在一个启用中的平台上：凭空写进来的 provider 会让模型在选择列表里
    // 挂到空分组、也无法用于创建知识库，历史上就是测试直接调 DAO 留下的脏数据。
    if (provider == null
        || Db.queryLong("select count(*) from moss_kb_model_provider where provider=? and enabled=true", provider) <= 0) {
      throw new IllegalArgumentException("供应商不存在或已停用：" + provider);
    }

    Row record = new Row();
    record.set("name", modelVo.getName()).set("model_type", modelVo.getModel_type()).set("model_name", modelVo.getModel_name())
        //
        .set("permission_type", modelVo.getPermission_type())
        //
        .set("credential", modelVo.getCredential()).set("status", "SUCCESS");

    record.set("provider", provider);

    if (modelVo.getModel_params_form() != null) {
      record.set("model_params_form", modelVo.getModel_params_form());
    }
    Long id = modelVo.getId();
    if (id != null) {
      record.set("id", id);
      return Db.update(MossKbTableNames.moss_kb_model, "id", record, new String[] { "credential", "model_params_form" });
    } else {
      record.set("id", SnowflakeIdUtils.id()).set("user_id", userId);
      return Db.save(MossKbTableNames.moss_kb_model, record, new String[] { "credential", "model_params_form" });
    }
  }

}
