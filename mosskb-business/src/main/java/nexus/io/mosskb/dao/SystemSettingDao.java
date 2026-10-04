package nexus.io.mosskb.dao;

import org.postgresql.util.PGobject;

import com.jfinal.kit.Kv;

import nexus.io.db.activerecord.Db;
import nexus.io.mosskb.constant.MossKbTableNames;
import nexus.io.tio.utils.json.JsonUtils;

public class SystemSettingDao {

  public PGobject getRsa() {
    String sql = String.format("select meta from %s where type = 1", MossKbTableNames.system_setting);
    return Db.queryFirst(sql);
  }

  public void saveRsa(String privateKeyStr, String publicKeyStr) {
    Kv kv = Kv.by("key", privateKeyStr).set("value", publicKeyStr);
    // type 是这张表的主键，重复生成密钥时要覆盖同一行，不能插入第二行。
    String sql = String.format("""
        insert into %s (type, meta, create_time, update_time)
        values (1, ?::jsonb, now(), now())
        on conflict (type) do update
          set meta = excluded.meta,
              update_time = now()
        """, MossKbTableNames.system_setting);
    Db.update(sql, JsonUtils.toJson(kv));
  }
}
