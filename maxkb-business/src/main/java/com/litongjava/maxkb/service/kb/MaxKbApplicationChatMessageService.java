package com.litongjava.maxkb.service.kb;

import java.util.ArrayList;
import java.util.List;

import org.postgresql.util.PGobject;

import com.litongjava.chat.UniChatMessage;
import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.kit.PgObjectUtils;
import com.litongjava.maxkb.constant.MaxKbTableNames;
import com.litongjava.maxkb.model.MaxKbApplicationChat;
import com.litongjava.maxkb.model.MaxKbApplicationChatRecord;
import com.litongjava.maxkb.model.MaxKbApplicationTempSetting;
import com.litongjava.maxkb.service.ChatStreamCallCan;
import com.litongjava.maxkb.stream.ChatStreamCallbackImpl;
import com.litongjava.maxkb.utils.TokenCounter;
import com.litongjava.maxkb.vo.CredentialVo;
import com.litongjava.maxkb.vo.MaxKbApplicationVo;
import com.litongjava.maxkb.vo.MaxKbChatRequestVo;
import com.litongjava.maxkb.vo.MaxKbChatStep;
import com.litongjava.maxkb.vo.MaxKbDatasetSettingVo;
import com.litongjava.maxkb.vo.MaxKbModelSetting;
import com.litongjava.maxkb.vo.MaxKbRetrieveResult;
import com.litongjava.maxkb.vo.ParagraphSearchResultVo;
import com.litongjava.model.result.ResultVo;
import com.litongjava.openai.chat.OpenAiChatRequest;
import com.litongjava.openai.client.OpenAiClient;
import com.litongjava.openai.consts.OpenAiConst;
import com.litongjava.openai.consts.OpenAiModels;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.utils.json.JsonUtils;
import com.litongjava.tio.utils.snowflake.SnowflakeIdUtils;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;

@Slf4j
public class MaxKbApplicationChatMessageService {

  MaxKbParagraphRetrieveService maxKbParagraphSearchService = Aop.get(MaxKbParagraphRetrieveService.class);

  public ResultVo ask(ChannelContext channelContext, Long chatId, MaxKbChatRequestVo vo) {
    String quesiton = vo.getMessage();
    if (StrUtil.isBlank(quesiton)) {
      return ResultVo.fail("The quesiton cannot be empty");
    }

    // 确定聊天类型
    Row quereyRecord = Row.by("id", chatId).set("is_deleted", false);
    Row record = Db.findFirst(MaxKbApplicationChat.tableName, "application_id,chat_type", quereyRecord);
    Long application_id = record.getLong("application_id");
    Integer chat_type = record.getInt("chat_type");
    log.info("application_id:{},chat_type:{}", application_id, chat_type);

    MaxKbApplicationVo applicationVo = null;
    if (chat_type == 1) {
      PGobject pgObject = Db.queryPGobjectById(MaxKbApplicationTempSetting.tableName, "setting", application_id);
      applicationVo = PgObjectUtils.toBean(pgObject, MaxKbApplicationVo.class);
    } else {

    }

    long messageId = SnowflakeIdUtils.id();
    // 保存历史记录
    int countTokens = TokenCounter.countTokens(quesiton);
    Row chatRecord = Row.by("id", messageId).set("problem_text", quesiton).set("message_tokens", countTokens).set("chat_id", chatId);
    Db.save(MaxKbApplicationChatRecord.tableName, chatRecord);

    // 搜索相关片段,并拼接
    List<Long> dataset_id_list = applicationVo.getDataset_id_list();
    MaxKbDatasetSettingVo dataset_setting = applicationVo.getDataset_setting();
    Float similarity = 0.0f;
    Integer top_n = 10;
    if (dataset_setting != null) {
      similarity = dataset_setting.getSimilarity();
      top_n = dataset_setting.getTop_n();
    } else {
      log.error("dataset_setting is null:{}", applicationVo.getId());
    }

    Long[] datasetIdArray = dataset_id_list.toArray(new Long[0]);

    MaxKbRetrieveResult maxKbSearchStep = maxKbParagraphSearchService.retrieve(datasetIdArray, similarity, top_n, quesiton);
    chatWichApplication(channelContext, quesiton, applicationVo, chatId, messageId, maxKbSearchStep);

    return ResultVo.ok("");
  }

  private void chatWichApplication(ChannelContext channelContext, String quesiton, MaxKbApplicationVo applicationVo, Long chatId,
      long messageId, MaxKbRetrieveResult maxKbSearchStep) {
    List<ParagraphSearchResultVo> records = maxKbSearchStep.getParagraph_list();
    log.info("records size:{}", records.size());
    String xmlData = MaxKbParagraphXMLGenerator.generateXML(records);

    MaxKbModelSetting model_setting = applicationVo.getModel_setting();

    String prompt = model_setting.getPrompt();
    String userPrompt = prompt.replace("{data}", xmlData).replace("{question}", quesiton);

    StringBuffer messageText = new StringBuffer();
    String systemPrompt = model_setting.getSystem();
    messageText.append(systemPrompt);
    messageText.append(userPrompt);

    OpenAiChatRequest OpenAiChatRequest = new OpenAiChatRequest();
    UniChatMessage systemMessage = new UniChatMessage("system", systemPrompt);
    UniChatMessage userMessage = new UniChatMessage("user", userPrompt);

    List<UniChatMessage> messages = new ArrayList<>();
    messages.add(systemMessage);
    messages.add(userMessage);
    OpenAiChatRequest.fromMessages(messages);
    OpenAiChatRequest.setModel(systemPrompt);

    // 获取模型
    Long model_id = applicationVo.getModel_id();
    String api_key = null;
    String api_base = null;
    String modelName = null;

    if (model_id != null) {
      Row modelRecord = Db.findById(MaxKbTableNames.max_kb_model, model_id);
      CredentialVo crdentianlVo = null;
      if (modelRecord != null) {
        Object credential = modelRecord.getColumns().remove("credential");
        if (credential instanceof String) {
          String credentialStr = (String) credential;
          crdentianlVo = JsonUtils.parse(credentialStr, CredentialVo.class);
          api_key = crdentianlVo.getApi_key();
          api_base = crdentianlVo.getApi_base();
        }
        if (api_base == null || api_key == null) {
          api_key = EnvUtils.get("OPENAI_API_KEY");
          api_base = OpenAiConst.API_PREFIX_URL;
        }

        modelName = modelRecord.getStr("model_name");
      }
    } else {
      api_key = EnvUtils.get("OPENAI_API_KEY");
      api_base = OpenAiConst.API_PREFIX_URL;
      modelName = OpenAiModels.GPT_4O_MINI;
    }

    OpenAiChatRequest.setModel(modelName);
    OpenAiChatRequest.setStream(true);

    MaxKbChatStep maxKbChatStep = new MaxKbChatStep();
    int message_tokens = TokenCounter.countTokens(messageText.toString());
    maxKbChatStep.setStep_type("step_type").setCost(0).setModel_id(model_id)
        //
        .setMessage_list(messages).setMessage_tokens(message_tokens);

    long start = System.currentTimeMillis();
    Callback callback = new ChatStreamCallbackImpl(chatId, messageId, start, maxKbSearchStep, maxKbChatStep, channelContext);
    Call call = OpenAiClient.chatCompletions(api_base, api_key, OpenAiChatRequest, callback);
    ChatStreamCallCan.put(chatId, call);
  }

}
