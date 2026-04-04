package nexus.io.maxkb.service;

import org.postgresql.util.PGobject;

import nexus.io.jfinal.aop.Aop;
import nexus.io.maxkb.dao.SystemSettingDao;

public class SystemSettingService {

  /**
   * 从数据库中获取 RSA 密钥对
   * 
   * @return 密钥对的 JSON 格式
   */
  public static String getKeyPairBySql() {
    PGobject pgObject = Aop.get(SystemSettingDao.class).getRsa();
    if(pgObject!=null) {
      
    }
    return null;
  }
}
