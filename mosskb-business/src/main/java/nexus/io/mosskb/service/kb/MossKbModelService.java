package nexus.io.mosskb.service.kb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.postgresql.util.PGobject;

import com.jfinal.kit.Kv;

import lombok.extern.slf4j.Slf4j;
import nexus.io.chat.PlatformInput;
import nexus.io.chat.UniChatMessage;
import nexus.io.db.activerecord.Db;
import nexus.io.db.activerecord.Row;
import nexus.io.jfinal.aop.Aop;
import nexus.io.mosskb.constant.MossKbTableNames;
import nexus.io.mosskb.dao.ModelDao;
import nexus.io.mosskb.model.MossKbModel;
import nexus.io.mosskb.vo.CredentialVo;
import nexus.io.mosskb.vo.ModelVo;
import nexus.io.model.result.ResultVo;
import nexus.io.openai.client.OpenAiClient;
import nexus.io.tio.utils.json.JsonUtils;
import okhttp3.Response;

@Slf4j
public class MossKbModelService {

  /**
   * @param name
   * @return
   */
  public ResultVo list(String name) {
    return list(name, null);
  }

  public ResultVo list(String name, String modelType) {
    MossKbUserService mossKbUserService = Aop.get(MossKbUserService.class);

    String[] jsonFields = new String[] { "meta" };
    // 只列出平台接入仍然有效的模型：平台被停用或删除后，其模型无法再被选用，
    // 留在列表里只会造成重名与无法创建知识库的困惑。选择界面按 provider 分组，
    // provider 不存在的模型会在下拉里挂到一个空分组下面。
    String columns = "select m.id,m.provider,m.name,m.model_type,m.model_name,m.status,m.meta,m.permission_type,m.user_id from %s m "
        + "where exists(select 1 from moss_kb_model_provider p where p.provider=m.provider and p.enabled=true)";
    String sql = null;
    if (name == null) {
      sql = String.format(columns, MossKbTableNames.moss_kb_model);
      List<Row> list = Db.findWithJsonField(sql, jsonFields);
      List<Kv> kvs = new ArrayList<>();
      for (Row r : list) {
        if (!ModelAccess.canUse(nexus.io.tio.boot.http.TioRequestContext.getUserIdLong(), r.getLong("id"))) {
          continue;
        }
        if (modelType != null && !modelType.equals(r.getStr("model_type"))) {
          continue;
        }
        Kv kv = r.toKv();
        kv.set("id", kv.get("id").toString());
        kv.set("user_id", kv.get("user_id").toString());
        Object meta = kv.get("meta");
        if (meta instanceof PGobject) {
          kv.set("meta", JsonUtils.parseObject(((PGobject) meta).getValue()));
        } else if (meta == null) {
          kv.set("meta", Kv.create());
        }

        String username = mossKbUserService.queryUsername(kv.getLong("user_id"));
        kv.set("username", username);

        kvs.add(kv);
      }
      return ResultVo.ok(kvs);
    }

    sql = String.format(columns, MossKbTableNames.moss_kb_model) + " and m.name=?";

    List<Kv> kvs = new ArrayList<>();
    List<Row> list = Db.findWithJsonField(sql, jsonFields, name);
    for (Row record : list) {
      if (!ModelAccess.canUse(nexus.io.tio.boot.http.TioRequestContext.getUserIdLong(), record.getLong("id"))) {
        continue;
      }
      if (modelType != null && !modelType.equals(record.getStr("model_type"))) {
        continue;
      }
      Kv kv = record.toKv();
      String username = mossKbUserService.queryUsername(kv.getLong("user_id"));
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
    if (modelVo.getId() != null && !ModelAccess.owns(userId, modelVo.getId())) {
      return ResultVo.fail("无权修改模型");
    }
    String name = modelVo.getName();
    if (name == null || name.isBlank() || name.length() > 64) {
      return ResultVo.fail(400, "模型名称须为 1 至 64 个字符");
    }
    if (modelVo.getId() == null && Db.exists(MossKbTableNames.moss_kb_model, "name", name)) {
      return ResultVo.fail(400, "模型名称【" + name + "】已存在");
    }

    try {
      ModelCatalogService catalog = Aop.get(ModelCatalogService.class);
      Row provider = catalog.provider(modelVo.getProvider());
      if (provider == null) {
        return ResultVo.fail(400, "供应商不存在或已停用");
      }
      if (!List.of("LLM", "EMBEDDING").contains(modelVo.getModel_type()) || !List.of("PRIVATE", "PUBLIC").contains(modelVo.getPermission_type())) {
        return ResultVo.fail(400, "模型类型或权限参数无效");
      }
      modelVo.setModel_name(ModelCatalogService.modelId(modelVo.getModel_name()));
      CredentialVo credential = modelVo.getCredential();
      if (credential == null) {
        return ResultVo.fail(400, "请填写模型凭据");
      }
      if (credential.getApi_base() == null || credential.getApi_base().isBlank()) {
        credential.setApi_base(provider.getStr("api_base"));
      }
      credential.setApi_base(ModelCatalogService.baseUrl(credential.getApi_base()));
      String key = credential.getApi_key();
      if (modelVo.getId() != null && (key == null || key.isBlank() || key.contains("*"))) {
        CredentialVo previous = ModelCatalogService.credential(Db.findById(MossKbTableNames.moss_kb_model, modelVo.getId()));
        if (!credential.getApi_base().equals(ModelCatalogService.baseUrl(previous.getApi_base()))) {
          return ResultVo.fail(400, "修改 API 域名时必须重新填写该平台的 API Key");
        }
        credential.setApi_key(previous.getApi_key());
      }
      if (credential.getApi_key() == null || credential.getApi_key().isBlank() || credential.getApi_key().contains("*")) {
        return ResultVo.fail(400, "请填写该平台的 API Key");
      }
      if (modelVo.getId() != null) {
        Row previous = Db.findById(MossKbTableNames.moss_kb_model, modelVo.getId());
        CredentialVo oldCredential = ModelCatalogService.credential(previous);
        if ("EMBEDDING".equals(previous.getStr("model_type"))
            && (!modelVo.getModel_name().equals(previous.getStr("model_name")) || !modelVo.getModel_type().equals(previous.getStr("model_type"))
                || !credential.getApi_base().equals(ModelCatalogService.baseUrl(oldCredential.getApi_base())))
            && Db.queryLong("select count(*) from moss_kb_dataset where embedding_mode_id=?", modelVo.getId()) > 0) {
          return ResultVo.fail(400, "被知识库使用的向量模型不能直接更换模型 ID 或 API 地址，请新建模型和知识库");
        }
      }
      try {
        validateModel(modelVo);
      } catch (Exception e) {
        return ResultVo.fail(400, "模型校验失败，请检查模型 ID、API 地址、密钥、额度；向量模型须支持 1024 维");
      }
    } catch (IllegalArgumentException e) {
      return ResultVo.fail(400, e.getMessage());
    } catch (Exception e) {
      return ResultVo.fail(400, "模型校验失败，请检查模型 ID、API 地址、密钥及平台额度");
    }

    Aop.get(ModelDao.class).saveOrUpdate(userId, modelVo);
    return ResultVo.ok();
  }

  static void validateModel(ModelVo model) throws IOException {
    CredentialVo credential = model.getCredential();
    if ("EMBEDDING".equals(model.getModel_type())) {
      com.alibaba.fastjson2.JSONObject input = new com.alibaba.fastjson2.JSONObject();
      input.put("model", model.getModel_name());
      input.put("input", List.of("模型连接测试"));
      input.put("dimensions", 1024);
      try (Response response = OpenAiClient.embeddings(credential.getApi_base(), credential.getApi_key(), input.toJSONString())) {
        if (!response.isSuccessful() || response.body() == null) {
          throw new IllegalArgumentException("向量模型校验失败，HTTP " + response.code());
        }
        float[] vector = com.alibaba.fastjson2.JSON.parseObject(response.body().string()).getJSONArray("data").getJSONObject(0).getObject("embedding", float[].class);
        if (vector == null || vector.length != 1024) {
          throw new IllegalArgumentException("当前知识库需要 1024 维向量，请选用支持 dimensions=1024 的模型");
        }
        for (float value : vector) {
          if (!Float.isFinite(value)) {
            throw new IllegalArgumentException("向量模型返回无效数据");
          }
        }
      }
    } else {
      nexus.io.chat.UniChatRequest request = new nexus.io.chat.UniChatRequest();
      request.setModel(model.getModel_name()).setApiPrefixUrl(credential.getApi_base()).setApiKey(credential.getApi_key())
          .setStream(false).setMessages(List.of(new UniChatMessage("user", "Reply OK")));
      nexus.io.chat.UniChatResponse response = nexus.io.chat.UniChatClient.generate(request);
      if (response == null || response.getRawData() == null) {
        throw new IllegalArgumentException("模型未返回有效响应");
      }
    }
  }

  public ResultVo delete(Long id) {
    if (Db.queryLong("select count(*) from moss_kb_dataset where embedding_mode_id=?", id) > 0
        || Db.queryLong("select count(*) from moss_kb_application where model_id=?", id) > 0) {
      return ResultVo.fail(400, "该模型仍被应用或知识库使用");
    }
    boolean ok = Aop.get(ModelDao.class).deleteById(id);
    if (ok) {
      return ResultVo.ok();
    } else {
      return ResultVo.fail();
    }
  }

  public ResultVo get(Long id) {
    Row record = Db.findById(MossKbTableNames.moss_kb_model, id);
    if (record == null) {
      return ResultVo.fail(404, "模型不存在");
    }
    CredentialVo credential = ModelCatalogService.credential(record);
    record.getColumns().remove("credential");
    Kv kv = record.toKv();
    credential.setApi_key("********");
    kv.set("credential", credential);
    kv.set("id", String.valueOf(id));
    kv.set("model_params_form", com.alibaba.fastjson2.JSON.parseArray(record.getStr("model_params_form")));
    return ResultVo.ok(kv);
  }

  public PlatformInput getEmbeddingPlatformInput(Long embedding_mode_id) {
    String sqlModelName = String.format("SELECT provider,model_name FROM %s WHERE id = ?",
        MossKbTableNames.moss_kb_model);
    MossKbModel mossKbModel = MossKbModel.dao.findFirst(sqlModelName, embedding_mode_id);
    String embeddingPlatformName = null;
    String embeddingModel = null;
    if (mossKbModel != null) {
      embeddingPlatformName = mossKbModel.getProvider();
      embeddingModel = mossKbModel.getModelName();
    } else {
      embeddingPlatformName = MossKbEnvUtils.getEmbeddingPlatform();
      embeddingModel = MossKbEnvUtils.getEmbeddingModel();
    }
    PlatformInput platformInput = new PlatformInput(embeddingPlatformName, embeddingModel);
    return platformInput;
  }
}
