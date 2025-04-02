package com.litongjava.maxkb.service.kb;

import java.util.concurrent.locks.Lock;

import com.google.common.util.concurrent.Striped;
import com.litongjava.db.activerecord.Db;
import com.litongjava.maxkb.model.MaxKbParagraphSummaryCache;
import com.litongjava.openai.chat.OpenAiChatResponseVo;
import com.litongjava.openai.client.OpenAiClient;
import com.litongjava.template.PromptEngine;
import com.litongjava.tio.utils.crypto.Md5Utils;
import com.litongjava.tio.utils.snowflake.SnowflakeIdUtils;

public class MaxKbParagraphSummaryService {

  private static final Striped<Lock> locks = Striped.lock(1024);
  private static final String prompt = PromptEngine.renderToString("ParagraphSummaryPrompt.txt");
  String selectContentSql = "select content from max_kb_paragraph_summary_cache where md5=?";

  public String summary(String paragraphContent) {
    String md5 = Md5Utils.getMD5(paragraphContent);
    String summaryContent = Db.queryStr(selectContentSql, md5);
    if (summaryContent != null) {
      return summaryContent;
    }

    String newPrompt = prompt + "\r\n\r\n" + paragraphContent;
    Lock lock = locks.get(md5);
    lock.lock();
    try {
      summaryContent = Db.queryStr(selectContentSql, md5);
      if (summaryContent != null) {
        return summaryContent;
      }

      OpenAiChatResponseVo chat = null;
      long start = System.currentTimeMillis();
      try {
        chat = OpenAiClient.chat(newPrompt);
      } catch (Exception e) {
        try {
          chat = OpenAiClient.chat(newPrompt);
        } catch (Exception e1) {
          chat = OpenAiClient.chat(newPrompt);
        }
      }
      long end = System.currentTimeMillis();
      if (chat != null) {
        summaryContent = chat.getChoices().get(0).getMessage().getContent();
        MaxKbParagraphSummaryCache model = new MaxKbParagraphSummaryCache();
        model.setId(SnowflakeIdUtils.id()).setMd5(md5).setSrc(paragraphContent).setContent(summaryContent);
        model.setElapsed((end - start)).setSystemFingerprint(chat.getSystem_fingerprint()).setModel(chat.getModel())
            //
            .setCompletionTokens(chat.getUsage().getCompletion_tokens())
            //
            .setPromptTokens(chat.getUsage().getPrompt_tokens())
            //
            .setTotalTokens(chat.getUsage().getTotal_tokens());
        model.save();
        return summaryContent;
      }
    } finally {
      lock.unlock();
    }
    return null;
  }
}
