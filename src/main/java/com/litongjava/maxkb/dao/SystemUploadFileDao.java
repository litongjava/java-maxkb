package com.litongjava.maxkb.dao;

import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Record;

public class SystemUploadFileDao {
  public static final String tableName = "max_kb_file";

  public Record getFileBasicInfoByMd5(String bucketName, String md5) {
    String sql = String.format("select id,filename,bucket_name,target_name from %s where bucket_name=? and md5=? and deleted=0", tableName);
    return Db.findFirst(sql, bucketName, md5);
  }

  public Record getFileBasicInfoById(long id) {
    String sql = String.format("select md5,filename,bucket_name,target_name from %s where id=? and deleted=0", tableName);
    return Db.findFirst(sql, id);

  }

  public boolean save(long id, String md5, String originFilename, int fileSize, String platform, String bucketName,
      //
      String targetName) {
    Record record = Record.by("id", id)
        //
        .set("md5", md5).set("filename", originFilename).set("file_size", fileSize)
        //
        .set("platform", platform).set("bucket_name", bucketName)
        //
        .set("target_name", targetName);

    return Db.save(tableName, record);
  }
}
