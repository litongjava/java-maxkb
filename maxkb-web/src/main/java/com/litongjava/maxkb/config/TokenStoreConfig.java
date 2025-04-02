package com.litongjava.maxkb.config;

import com.litongjava.annotation.AConfiguration;
import com.litongjava.annotation.Initialization;
import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.maxkb.constant.MaxKbTableNames;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.token.ITokenStorage;
import com.litongjava.tio.utils.token.TokenManager;

@AConfiguration
public class TokenStoreConfig {

  @Initialization
  public void config() {
    if(EnvUtils.isDev()) {
      TokenManager.setTokenStorage(new ITokenStorage() {

        @Override
        public String remove(Object userId) {
          String sql = String.format("select token from %s where id=?", MaxKbTableNames.max_kb_user_token);
          String token = Db.queryStr(sql, userId);
          Db.deleteById(MaxKbTableNames.max_kb_user_token, userId);
          return token;
        }

        @Override
        public void put(Object userId, String tokenValue) {
          Db.deleteById(MaxKbTableNames.max_kb_user_token, userId);
          Db.save(MaxKbTableNames.max_kb_user_token, Row.by("id", userId).set("token", tokenValue));
        }

        @Override
        public boolean containsKey(Object userId) {
          return Db.exists(MaxKbTableNames.max_kb_user_token, "id", userId);
        }
      });
    }

  }
}
