package com.litongjava.maxkb.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.postgresql.util.PGobject;

import com.jfinal.kit.Kv;
import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.constant.TableNames;
import com.litongjava.maxkb.dao.ModelDao;
import com.litongjava.maxkb.enumeration.ModelProvider;
import com.litongjava.maxkb.enumeration.ModelType;
import com.litongjava.maxkb.vo.CredentialVo;
import com.litongjava.maxkb.vo.ModelVo;
import com.litongjava.model.result.ResultVo;
import com.litongjava.openai.chat.ChatMessage;
import com.litongjava.openai.chat.OpenAiChatRequestVo;
import com.litongjava.openai.client.OpenAiClient;
import com.litongjava.openai.constants.OpenAiModels;
import com.litongjava.openai.embedding.EmbeddingRequestVo;
import com.litongjava.tio.utils.hutool.DataMaskingUtil;
import com.litongjava.tio.utils.json.JsonUtils;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;

@Slf4j
public class MaxKbModelService {

  /**
   * @param name
   * @return
   */
  public ResultVo list(String name) {
    MaxKbUserService maxKbUserService = Aop.get(MaxKbUserService.class);

    String[] jsonFields = new String[] { "meta" };
    String sql = null;
    if (name == null) {
      sql = "select id,provider,name,model_type,model_name,status,meta,permission_type,user_id from %s";
      sql = String.format(sql, TableNames.max_kb_model);
      List<Row> list = Db.findWithJsonField(sql, jsonFields);
      List<Kv> kvs = new ArrayList<>();
      for (Row r : list) {
        Kv kv = r.toKv();
        kv.set("id", kv.get("id").toString());
        kv.set("user_id", kv.get("user_id").toString());
        PGobject meta = kv.getAs("meta");
        if (meta == null || meta.isNull()) {
          kv.set("meta", "{}");
        } else {
          kv.set("meta", JsonUtils.parseObject(meta.getValue()));
        }

        String username = maxKbUserService.queryUsername(kv.getLong("user_id"));
        kv.set("username", username);

        kvs.add(kv);
      }
      return ResultVo.ok(kvs);
    }

    sql = "select id,provider,name,model_type,model_name,status,meta,permission_type,user_id from %s where name=?";
    sql = String.format(sql, TableNames.max_kb_model);

    List<Kv> kvs = new ArrayList<>();
    List<Row> list = Db.findWithJsonField(sql, jsonFields, name);
    for (Row record : list) {
      Kv kv = record.toKv();
      String username = maxKbUserService.queryUsername(kv.getLong("user_id"));
      kv.set("username", username);
      kvs.add(kv);
    }
    return ResultVo.ok(kvs);
  }

  /**
   * 
   * @param map {"name":"text-embedding-3-large","model_type":"EMBEDDING","model_name":"text-embedding-3-large","permission_type":"PRIVATE","credential":{"api_base":"https://api.openai.com/v1","api_key":"11111111"},"provider":"model_openai_provider"}
   * @return
   */
  public ResultVo save(Long userId, ModelVo modelVo) {
    String name = modelVo.getName();
    log.info("name:{}", name);
    if (modelVo.getId()==null && Db.exists(TableNames.max_kb_model, "name", name)) {
      return ResultVo.fail(400, "模型名称【" + name + "】已存在");
    }

    String model_type = modelVo.getModel_type();
    String provider = modelVo.getProvider();

    if (ModelProvider.model_openai_provider.getName().equals(provider)) {
      if (ModelType.EMBEDDING.getName().equals(model_type)) {
        EmbeddingRequestVo embeddingRequestVo = new EmbeddingRequestVo();
        embeddingRequestVo.input("Hi").model(OpenAiModels.text_embedding_3_small);

        String api_base = modelVo.getCredential().getApi_base();
        String api_key = modelVo.getCredential().getApi_key();

        String bodyString = JsonUtils.toJson(embeddingRequestVo);
        // send request
        try (Response response = OpenAiClient.embeddings(api_base, api_key, bodyString)) {
          if (!response.isSuccessful()) {
            // get response string
            String string = response.body().string();
            return ResultVo.fail(400, "校验失败,请检查参数是否正确:" + string);
          }
        } catch (IOException e) {
          e.printStackTrace();
          return ResultVo.fail(500, e.getMessage());
        }
      } else {
        if (ModelType.LLM.getName().equals(model_type)) {
          String api_base = modelVo.getCredential().getApi_base();
          String api_key = modelVo.getCredential().getApi_key();

          // messages
          List<ChatMessage> messages = new ArrayList<>();
          ChatMessage message = new ChatMessage().role("user").content("hi");
          messages.add(message);

          OpenAiChatRequestVo openAiChatRequestVo = new OpenAiChatRequestVo();
          openAiChatRequestVo.setStream(false);
          openAiChatRequestVo.setModel(OpenAiModels.gpt_4o_mini);
          openAiChatRequestVo.fromMessages(messages);

          String bodyString = JsonUtils.toJson(openAiChatRequestVo);
          // send request
          try (Response response = OpenAiClient.chatCompletions(api_base, api_key, bodyString)) {
            if (!response.isSuccessful()) {
              // get response string
              String string = response.body().string();
              return ResultVo.fail(500, "校验失败,请检查参数是否正确:" + string);
            }
          } catch (IOException e) {
            e.printStackTrace();
            return ResultVo.fail(500, e.getMessage());
          }
        }
      }
    }

    Aop.get(ModelDao.class).saveOrUpdate(userId, modelVo);
    return ResultVo.ok();
  }

  public ResultVo delete(Long id) {
    boolean ok = Aop.get(ModelDao.class).deleteById(id);
    if (ok) {
      return ResultVo.ok();
    } else {
      return ResultVo.fail();
    }
  }

  public ResultVo get(Long id) {
    Row record = Db.findById(TableNames.max_kb_model, id);
    Object credential = record.getColumns().remove("credential");
    Kv kv = record.toKv();
    if (credential instanceof String) {
      String credentialStr = (String) credential;
      CredentialVo crdentianlVo = JsonUtils.parse(credentialStr, CredentialVo.class);
      String api_key = crdentianlVo.getApi_key();
      crdentianlVo.setApi_key(DataMaskingUtil.maskApiKey(api_key));
      kv.set("credential", crdentianlVo);
    }
    return ResultVo.ok(kv);
  }
}
