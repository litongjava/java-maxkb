package com.litongjava.maxkb.model;

import org.junit.Test;

import com.litongjava.maxkb.config.DbConfig;
import com.litongjava.tio.boot.testing.TioBootTest;
import com.litongjava.tio.utils.json.JsonUtils;
import com.litongjava.tio.utils.snowflake.SnowflakeIdUtils;

public class MaxKbFileTest {

  @Test
  public void test() {
    TioBootTest.runWith(DbConfig.class);
    MaxKbFile maxKbFile = new MaxKbFile();
    maxKbFile.setId(SnowflakeIdUtils.id());
    maxKbFile.setMd5("001");
    maxKbFile.setFilename("001").setFileSize(10000L);
    maxKbFile.setPlatform("local");
    maxKbFile.setBucketName("bucket_name");
    maxKbFile.setTargetName("target_name");
    
    maxKbFile.save();
    String json = JsonUtils.toJson(maxKbFile);
    System.out.println(json);
  }

}
