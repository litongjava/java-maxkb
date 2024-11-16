package com.litongjava.maxkb.dao;

import org.junit.Test;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.MaxKbApp;
import com.litongjava.maxkb.vo.CredentialVo;
import com.litongjava.maxkb.vo.ModelVo;
import com.litongjava.tio.boot.testing.TioBootTest;

public class ModelDaoTest {

  @Test
  public void test() {
    TioBootTest.run(MaxKbApp.class);
    //String id="42f63a3d-427e-11ef-b3ec-a8a1595801ab";
    //String id="6a495cb8-74a4-11ef-9dfa-024280800003";
    Long id = 0L;
    Aop.get(ModelDao.class).deleteById(id);
  }

  @Test
  public void testSave() {
    TioBootTest.run(MaxKbApp.class);
    ModelVo modelVo = new ModelVo();
    modelVo.setName("text-embedding-3-large");
    modelVo.setModel_type("EMBEDDING");
    modelVo.setModel_name("text-embedding-3-large").setPermission_type("PRIVATE").setProvider("provider");
    CredentialVo credentialVo = new CredentialVo("https://api.openai.com", "xxxx");
    modelVo.setCredential(credentialVo);
    Long userId = 0L;
    Aop.get(ModelDao.class).saveOrUpdate(userId, modelVo);
  }

}
