package com.litongjava.maxkb.service.kb;

import java.util.Arrays;

import org.postgresql.util.PGobject;

import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.db.utils.PgVectorUtils;
import com.litongjava.maxkb.constant.MaxKbTableNames;
import com.litongjava.openai.client.OpenAiClient;
import com.litongjava.tio.utils.crypto.Md5Utils;
import com.litongjava.tio.utils.snowflake.SnowflakeIdUtils;

public class MaxKbEmbeddingService {
  private final Object vectorLock = new Object();
  private final Object writeLock = new Object();

  public PGobject getVector(String text, String model) {
    String v = null;
    String md5 = Md5Utils.getMD5(text);
    String sql = String.format("select v from %s where md5=? and m=?", MaxKbTableNames.max_kb_embedding_cache);
    PGobject pGobject = Db.queryFirst(sql, md5, model);

    if (pGobject == null) {
      float[] embeddingArray = null;
      try {
        embeddingArray = OpenAiClient.embeddingArray(text, model);
      } catch (Exception e) {
        try {
          embeddingArray = OpenAiClient.embeddingArray(text, model);
        } catch (Exception e1) {
          embeddingArray = OpenAiClient.embeddingArray(text, model);
        }
      }

      String string = Arrays.toString(embeddingArray);
      long id = SnowflakeIdUtils.id();
      v = (String) string;
      pGobject = PgVectorUtils.getPgVector(v);
      Row saveRecord = new Row().set("t", text).set("v", pGobject).set("id", id).set("md5", md5)
          //
          .set("m", model);
      synchronized (writeLock) {
        Db.save(MaxKbTableNames.max_kb_embedding_cache, saveRecord);
      }
    }
    return pGobject;
  }

  public Long getVectorId(String text, String model) {
    String md5 = Md5Utils.getMD5(text);
    String sql = String.format("select id from %s where md5=? and m=?", MaxKbTableNames.max_kb_embedding_cache);
    Long id = Db.queryLong(sql, md5, model);

    if (id == null) {
      float[] embeddingArray = null;
      synchronized (vectorLock) {
        embeddingArray = OpenAiClient.embeddingArray(text, model);
      }

      String vString = Arrays.toString(embeddingArray);
      id = SnowflakeIdUtils.id();
      PGobject pGobject = PgVectorUtils.getPgVector(vString);
      Row saveRecord = new Row().set("t", text).set("v", pGobject).set("id", id).set("md5", md5)
          //
          .set("m", model);
      synchronized (writeLock) {
        Db.save(MaxKbTableNames.max_kb_embedding_cache, saveRecord);
      }
    }
    return id;
  }

}
