package com.litongjava.maxkb.dao;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import org.junit.Test;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.MaxKbApp;
import com.litongjava.tio.boot.testing.TioBootTest;
import com.litongjava.tio.utils.crypto.RsaUtils;

public class SystemSettingDaoTest {

  @Test
  public void test() {
    TioBootTest.run(MaxKbApp.class);
    KeyPair pair = RsaUtils.generateKeyPair();
    PublicKey publicKey = pair.getPublic();
    PrivateKey privateKey = pair.getPrivate();

    String privateKeyStr = Base64.getEncoder().encodeToString(privateKey.getEncoded());
    String publicKeyStr = Base64.getEncoder().encodeToString(publicKey.getEncoded());

    Aop.get(SystemSettingDao.class).saveRsa(privateKeyStr, publicKeyStr);
  }
}
