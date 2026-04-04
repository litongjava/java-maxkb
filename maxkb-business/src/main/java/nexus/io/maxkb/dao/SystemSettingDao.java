package nexus.io.maxkb.dao;

import java.sql.SQLException;
import java.util.Date;

import org.postgresql.util.PGobject;

import com.jfinal.kit.Kv;

import nexus.io.db.activerecord.Db;
import nexus.io.db.activerecord.Row;
import nexus.io.maxkb.constant.MaxKbTableNames;
import nexus.io.tio.utils.json.JsonUtils;

public class SystemSettingDao {

  public PGobject getRsa() {
    String sql = String.format("select meta from %s where type = 1", MaxKbTableNames.system_setting);
    return Db.queryFirst(sql);
  }

  public void saveRsa(String privateKeyStr, String publicKeyStr) {
    Kv kv = Kv.by("key", privateKeyStr).set("value", publicKeyStr);
    PGobject meta = new PGobject();
    try {
      meta.setType("jsonb");
      meta.setValue(JsonUtils.toJson(kv));
    } catch (SQLException e) {
      e.printStackTrace();
    }
    Row record = Row.by("meta", meta).set("type", 1);
    //timestamptz
    record.set("create_time", new Date());
    record.set("update_time", new Date());
    Db.save(MaxKbTableNames.system_setting, record);
  }
}
