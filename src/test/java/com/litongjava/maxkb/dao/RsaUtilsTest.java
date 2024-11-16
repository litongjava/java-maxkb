package com.litongjava.maxkb.dao;

import java.security.KeyPair;
import java.security.PrivateKey;

import org.junit.Test;

import com.litongjava.tio.utils.crypto.RsaUtils;

public class RsaUtilsTest {

  @Test
  public void test() {
    KeyPair pair = RsaUtils.generateKeyPair();
    String privateKeyString = RsaUtils.getPrivateKeyString(pair);
    System.out.println(privateKeyString);
    PrivateKey privateKey = RsaUtils.getPrivateKeyFromString(privateKeyString);
    System.out.println(privateKey);
  }
}
