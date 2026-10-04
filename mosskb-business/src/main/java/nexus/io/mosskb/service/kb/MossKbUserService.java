package nexus.io.mosskb.service.kb;

import nexus.io.db.activerecord.Db;
import nexus.io.ehcache.EhCacheKit;
import nexus.io.mosskb.model.MossKbModel;
import nexus.io.mosskb.model.MossKbUser;

public class MossKbUserService {
  public String queryUsername(Long user_id) {
    String cacheName = MossKbModel.tableName + "_username";
    String username = EhCacheKit.get(cacheName, user_id);
    if (username == null) {
      String sql = String.format("select username from %s where id=?", MossKbUser.tableName);
      username = Db.queryStr(sql, user_id);
      EhCacheKit.put(cacheName, user_id, username, 60);
    }
    return username;
  }
}
