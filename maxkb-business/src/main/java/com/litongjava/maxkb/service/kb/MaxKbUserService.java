package com.litongjava.maxkb.service.kb;

import com.litongjava.maxkb.model.MaxKbModel;
import com.litongjava.maxkb.model.MaxKbUser;

import nexus.io.db.activerecord.Db;
import nexus.io.ehcache.EhCacheKit;

public class MaxKbUserService {
  public String queryUsername(Long user_id) {
    String cacheName = MaxKbModel.tableName + "_username";
    String username = EhCacheKit.get(cacheName, user_id);
    if (username == null) {
      String sql = String.format("select username from %s where id=?", MaxKbUser.tableName);
      username = Db.queryStr(sql, user_id);
      EhCacheKit.put(cacheName, user_id, username, 60);
    }
    return username;
  }
}
