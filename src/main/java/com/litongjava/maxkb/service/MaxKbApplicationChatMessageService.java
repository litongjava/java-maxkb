package com.litongjava.maxkb.service;

import java.util.ArrayList;
import java.util.List;

import org.postgresql.util.PGobject;

import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Record;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.kit.PGJsonUtils;
import com.litongjava.maxkb.constant.TableNames;
import com.litongjava.maxkb.model.MaxKbApplicationChat;
import com.litongjava.maxkb.model.MaxKbApplicationChatRecord;
import com.litongjava.maxkb.model.MaxKbApplicationTempSetting;
import com.litongjava.maxkb.stream.ChatStreamCallbackImpl;
import com.litongjava.maxkb.utils.TokenCounter;
import com.litongjava.maxkb.vo.CredentialVo;
import com.litongjava.maxkb.vo.MaxKbApplicationVo;
import com.litongjava.maxkb.vo.MaxKbChatRequestVo;
import com.litongjava.maxkb.vo.MaxKbChatStep;
import com.litongjava.maxkb.vo.MaxKbDatasetSettingVo;
import com.litongjava.maxkb.vo.MaxKbModelSetting;
import com.litongjava.maxkb.vo.MaxKbSearchStep;
import com.litongjava.maxkb.vo.ParagraphSearchResultVo;
import com.litongjava.model.result.ResultVo;
import com.litongjava.openai.chat.ChatMessage;
import com.litongjava.openai.chat.OpenAiChatRequestVo;
import com.litongjava.openai.client.OpenAiClient;
import com.litongjava.openai.constants.OpenAiConstants;
import com.litongjava.openai.constants.OpenAiModels;
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

  MaxKbParagraphSearchService maxKbParagraphSearchService = Aop.get(MaxKbParagraphSearchService.class);

  public ResultVo ask(ChannelContext channelContext, Long chatId, MaxKbChatRequestVo vo) {
    String quesiton = vo.getMessage();
    if (StrUtil.isBlank(quesiton)) {
      return ResultVo.fail("The quesiton cannot be empty");
    }

    // 确定聊天类型
    Record quereyRecord = Record.by("id", chatId).set("is_deleted", false);
    Record record = Db.findFirst(MaxKbApplicationChat.tableName, "application_id,chat_type", quereyRecord);
    Long application_id = record.getLong("application_id");
    Integer chat_type = record.getInt("chat_type");
    log.info("application_id:{},chat_type:{}", application_id, chat_type);

    MaxKbApplicationVo applicationVo = null;
    if (chat_type == 1) {
      PGobject pgObject = Db.queryPGobjectById(MaxKbApplicationTempSetting.tableName, "setting", application_id);
      applicationVo = PGJsonUtils.toBean(pgObject, MaxKbApplicationVo.class);
    } else {

    }

    long messageId = SnowflakeIdUtils.id();
    // 保存历史记录
    int countTokens = TokenCounter.countTokens(quesiton);
    Record chatRecord = Record.by("id", messageId).set("problem_text", quesiton).set("message_tokens", countTokens).set("chat_id", chatId);
    Db.save(MaxKbApplicationChatRecord.tableName, chatRecord);

    //搜索相关片段,并拼接
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

    MaxKbSearchStep maxKbSearchStep = maxKbParagraphSearchService.search(datasetIdArray, similarity, top_n, quesiton);
    chatWichApplication(channelContext, quesiton, applicationVo, chatId, messageId, maxKbSearchStep);

    return ResultVo.ok("");
  }

  private void chatWichApplication(ChannelContext channelContext, String quesiton, MaxKbApplicationVo applicationVo, Long chatId, long messageId, MaxKbSearchStep maxKbSearchStep) {
    List<ParagraphSearchResultVo> records = maxKbSearchStep.getParagraph_list();
    log.info("records size:{}", records.size());
    StringBuffer data = new StringBuffer();
    data.append("<data>");
    for (ParagraphSearchResultVo paragraphSearchResultVo : records) {
      String content = paragraphSearchResultVo.getContent();
      data.append(content);
    }
    data.append("</data>");

    MaxKbModelSetting model_setting = applicationVo.getModel_setting();

    String prompt = model_setting.getPrompt();
    String userPrompt = prompt.replace("{data}", data.toString()).replace("{question}", quesiton);

    StringBuffer messageText = new StringBuffer();
    String systemPrompt = model_setting.getSystem();
    messageText.append(systemPrompt);
    messageText.append(userPrompt);

    OpenAiChatRequestVo openAiChatRequestVo = new OpenAiChatRequestVo();
    ChatMessage systemMessage = new ChatMessage("system", systemPrompt);
    ChatMessage userMessage = new ChatMessage("user", userPrompt);

    List<ChatMessage> messages = new ArrayList<>();
    messages.add(systemMessage);
    messages.add(userMessage);
    openAiChatRequestVo.setMessages(messages);
    openAiChatRequestVo.setModel(systemPrompt);

    //获取模型
    Long model_id = applicationVo.getModel_id();
    String api_key = null;
    String api_base = null;
    String modelName = null;

    if (model_id != null) {
      Record modelRecord = Db.findById(TableNames.max_kb_model, model_id);
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
          api_base = OpenAiConstants.api_perfix_url;
        }

        modelName = modelRecord.getStr("model_name");
      }
    } else {
      api_key = EnvUtils.get("OPENAI_API_KEY");
      api_base = OpenAiConstants.api_perfix_url;
      modelName = OpenAiModels.gpt_4o_mini;
    }

    openAiChatRequestVo.setModel(modelName);
    openAiChatRequestVo.setStream(true);

    MaxKbChatStep maxKbChatStep = new MaxKbChatStep();
    int message_tokens = TokenCounter.countTokens(messageText.toString());
    maxKbChatStep.setStep_type("step_type").setCost(0).setModel_id(model_id)
        //
        .setMessage_list(messages).setMessage_tokens(message_tokens);

    long start = System.currentTimeMillis();
    Callback callback = new ChatStreamCallbackImpl(chatId, messageId, start, maxKbSearchStep, maxKbChatStep, channelContext);
    Call call = OpenAiClient.chatCompletions(api_base, api_key, openAiChatRequestVo, callback);
    ChatStreamCallCan.put(chatId, call);
  }

}
