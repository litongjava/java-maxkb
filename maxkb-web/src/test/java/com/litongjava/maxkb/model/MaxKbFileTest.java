package com.litongjava.maxkb.model;

import org.junit.Test;

import nexus.io.maxkb.config.MaxKbDbConfig;
import nexus.io.maxkb.model.MaxKbFile;
import nexus.io.tio.boot.testing.TioBootTest;
import nexus.io.tio.utils.json.JsonUtils;
import nexus.io.tio.utils.snowflake.SnowflakeIdUtils;

public class MaxKbFileTest {

  @Test
  public void test() {
    TioBootTest.runWith(MaxKbDbConfig.class);
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
