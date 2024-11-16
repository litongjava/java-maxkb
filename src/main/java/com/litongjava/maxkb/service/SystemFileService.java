package com.litongjava.maxkb.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.litongjava.db.activerecord.Record;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.dao.SystemUploadFileDao;
import com.litongjava.maxkb.vo.UploadResultVo;
import com.litongjava.tio.http.common.UploadFile;
import com.litongjava.tio.utils.crypto.Md5Utils;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.hutool.FileUtil;
import com.litongjava.tio.utils.hutool.FilenameUtils;
import com.litongjava.tio.utils.snowflake.SnowflakeIdUtils;

public class SystemFileService {

  /**
   * 上传文件并存储到本地文件系统
   *
   * @param uploadFile  上传的文件
   * @param bucketName  存储桶名称
   * @param category    文件分类
   * @return UploadResultVo 文件上传结果
   */
  public UploadResultVo upload(UploadFile uploadFile, String bucketName, String category) {
    if (uploadFile != null) {
      byte[] fileData = uploadFile.getData();
      String digestHex = Md5Utils.digestHex(fileData);
      SystemUploadFileDao systemUploadFileDao = Aop.get(SystemUploadFileDao.class);
      Record record = systemUploadFileDao.getFileBasicInfoByMd5(bucketName, digestHex);

      // 如果文件已存在，返回已有文件信息
      if (record != null) {
        Long id = record.getLong("id");
        String filename = record.getStr("filename");
        String targetName = record.getStr("target_name");
        String url = getUrl(bucketName, targetName);
        return new UploadResultVo(id, filename, url, digestHex);
      }

      // 生成新的文件名和路径
      String originFilename = uploadFile.getName();
      String suffix = FilenameUtils.getSuffix(originFilename);
      long id = SnowflakeIdUtils.id();
      String filename = id + "." + suffix;
      Path path = Paths.get("pages", bucketName, category);

      // 创建目录（如果不存在）
      try {
        Files.createDirectories(path);
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }

      // 完整文件路径
      Path filePath = path.resolve(filename);
      File file = filePath.toFile();

      // 将文件数据写入指定路径
      FileUtil.writeBytes(fileData, file);

      String targetName = category + "/" + filename;
      String url = getUrl(bucketName, targetName);
      systemUploadFileDao.save(id, digestHex, originFilename, fileData.length, "local", bucketName, targetName);
      return new UploadResultVo(id, originFilename, url, digestHex);
    }
    return null;
  }

  /**
   * 根据存储桶和目标名称生成文件访问 URL
   *
   * @param bucketName 存储桶名称
   * @param targetName 目标名称
   * @return 文件访问 URL
   */
  public String getUrl(String bucketName, String targetName) {
    String prefixUrl = EnvUtils.getStr("file_prefix_url");
    return prefixUrl + "/" + bucketName + "/" + targetName;
  }

  /**
   * 根据文件 ID 获取文件 URL
   *
   * @param id 文件 ID
   * @return UploadResultVo 文件上传结果
   */
  public UploadResultVo getUrlById(Long id) {
    SystemUploadFileDao systemUploadFileDao = Aop.get(SystemUploadFileDao.class);
    Record record = systemUploadFileDao.getFileBasicInfoById(id);
    String md5 = record.getStr("md5");
    String filename = record.getStr("filename");
    String bucketName = record.getStr("bucket_name");
    String targetName = record.getStr("target_name");
    String url = getUrl(bucketName, targetName);
    return new UploadResultVo(id, filename, url, md5);
  }

  /**
   * 根据文件 MD5 获取文件 URL
   *
   * @param bucketName 存储桶名称
   * @param md5        文件 MD5 值
   * @return UploadResultVo 文件上传结果
   */
  public UploadResultVo getUrlByMd5(String bucketName, String md5) {
    SystemUploadFileDao systemUploadFileDao = Aop.get(SystemUploadFileDao.class);
    Record record = systemUploadFileDao.getFileBasicInfoByMd5(bucketName, md5);
    Long id = record.getLong("id");
    String filename = record.getStr("filename");
    String targetName = record.getStr("target_name");
    String url = getUrl(bucketName, targetName);
    return new UploadResultVo(id, filename, url, md5);
  }
}
