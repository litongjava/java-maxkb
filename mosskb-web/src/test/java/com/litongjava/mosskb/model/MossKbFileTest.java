package com.litongjava.mosskb.model;

import org.junit.Test;

import nexus.io.mosskb.config.MossKbDbConfig;
import nexus.io.mosskb.model.MossKbFile;
import nexus.io.tio.boot.testing.TioBootTest;
import nexus.io.tio.utils.json.JsonUtils;
import nexus.io.tio.utils.snowflake.SnowflakeIdUtils;

public class MossKbFileTest {

  @Test
  public void test() {
    TioBootTest.runWith(MossKbDbConfig.class);
    MossKbFile mossKbFile = new MossKbFile();
    mossKbFile.setId(SnowflakeIdUtils.id());
    mossKbFile.setMd5("001");
    mossKbFile.setFilename("001").setFileSize(10000L);
    mossKbFile.setPlatform("local");
    mossKbFile.setBucketName("bucket_name");
    mossKbFile.setTargetName("target_name");
    
    mossKbFile.save();
    String json = JsonUtils.toJson(mossKbFile);
    System.out.println(json);
  }

}
