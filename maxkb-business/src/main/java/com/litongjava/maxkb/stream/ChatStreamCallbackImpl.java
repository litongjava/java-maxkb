package com.litongjava.maxkb.stream;

import java.io.IOException;
import java.util.List;

import org.postgresql.util.PGobject;

import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.kit.PgObjectUtils;
import com.litongjava.maxkb.constant.SSEConstant;
import com.litongjava.maxkb.constant.MaxKbTableNames;
import com.litongjava.maxkb.service.ChatStreamCallCan;
import com.litongjava.maxkb.utils.TokenCounter;
import com.litongjava.maxkb.vo.MaxKbChatRecordDetail;
import com.litongjava.maxkb.vo.MaxKbChatStep;
import com.litongjava.maxkb.vo.MaxKbRetrieveResult;
import com.litongjava.maxkb.vo.MaxKbStreamChatVo;
import com.litongjava.openai.chat.ChatResponseDelta;
import com.litongjava.openai.chat.Choice;
import com.litongjava.openai.chat.OpenAiChatResponseVo;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.http.common.sse.SsePacket;
import com.litongjava.tio.http.server.util.SseEmitter;
import com.litongjava.tio.utils.json.FastJson2Utils;
import com.litongjava.tio.utils.json.JsonUtils;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Slf4j
public class ChatStreamCallbackImpl implements Callback {

  private final Long chatId;
  private final Long messageId;
  private final long start;
  private final MaxKbRetrieveResult searchStep;
  private final MaxKbChatStep chatStep;
  private final ChannelContext channelContext;

  public ChatStreamCallbackImpl(Long chatId, Long messageId, long start, MaxKbRetrieveResult searchStep, MaxKbChatStep chatStep,
      //
      ChannelContext channelContext) {
    this.chatId = chatId;
    this.messageId = messageId;
    this.start = start;
    this.searchStep = searchStep;
    this.chatStep = chatStep;
    this.channelContext = channelContext;

  }

  @Override
  public void onResponse(Call call, Response response) throws IOException {
    if (!response.isSuccessful()) {
      String message = "chatgpt response an unsuccessful message:" + response.body().string();
      log.error(message);
      SsePacket ssePacket = new SsePacket(SSEConstant.error, message);
      Tio.send(channelContext, ssePacket);
      cleanup(chatId);
      SseEmitter.closeChunkConnection(channelContext);
      return;
    }

    try (ResponseBody responseBody = response.body()) {
      if (responseBody == null) {
        String message = "response body is null";
        log.error(message);
        SseEmitter.pushSSEChunk(channelContext, "error", message);
        cleanup(chatId);
        return;
      }
      StringBuffer completionContent = processResponseBody(chatId, messageId, channelContext, responseBody);
      long elpased = System.currentTimeMillis() - start;
      double runTime = ((double) elpased) / 1000;
      int answer_tokens = TokenCounter.countTokens(completionContent.toString());
      chatStep.setRun_time(runTime).setAnswer_tokens(answer_tokens);

      MaxKbChatRecordDetail detail = new MaxKbChatRecordDetail(chatStep, searchStep);
      String json = JsonUtils.toJson(detail);
      PGobject pgobject = PgObjectUtils.json(json);
      Row record = Row.by("id", messageId).set("run_time", runTime).set("answer_text", completionContent.toString())
          //
          .set("answer_tokens", answer_tokens).set("message_tokens", chatStep.getMessage_tokens())
          //
          .set("details", pgobject);
      Db.update(MaxKbTableNames.max_kb_application_chat_record, record);

    } catch (Exception e) {
      String message = "chatgpt response an unsuccessful message:" + e.getMessage();
      SsePacket ssePacket = new SsePacket(SSEConstant.error, message);
      Tio.send(channelContext, ssePacket);
    } finally {
      cleanup(chatId);
      SseEmitter.closeChunkConnection(channelContext);
    }

  }

  @Override
  public void onFailure(Call call, IOException e) {
    String message = "error: " + e.getMessage();
    SseEmitter.pushSSEChunk(channelContext, "error", message);
    cleanup(chatId);
    SseEmitter.closeChunkConnection(channelContext);
  }

  public StringBuffer processResponseBody(Long chatId, Long messageId, ChannelContext channelContext, ResponseBody responseBody) throws IOException {
    StringBuffer completionContent = new StringBuffer();
    StringBuffer fnCallName = new StringBuffer();
    StringBuffer fnCallArgs = new StringBuffer();

    String line;
    while ((line = responseBody.source().readUtf8Line()) != null) {
      if (line.length() < 1) {
        continue;
      }

      // 因为原始数据是data:开头
      if (line.length() > 6) {
        processResponseChunk(chatId, messageId, channelContext, completionContent, line);
      }
    }

    // 发送一个大小为 0 的 chunk 以表示消息结束
    if (fnCallName.length() > 0) {
      // 获取数据
      String functionCallResult = functionCall(fnCallName, fnCallArgs);
      // 再次发送到大模型
      if (functionCallResult != null) {
        // 处理 functionCallResult 的逻辑
        // 例如，可以重新调用 stream 方法
      } else {
        long end = System.currentTimeMillis();
        log.info("finish llm in {} (ms):", (end - start));
        SseEmitter.closeChunkConnection(channelContext);
      }
    }
    return completionContent;
  }

  private void processResponseChunk(Long chatId, Long messageId, ChannelContext channelContext, StringBuffer completionContent, String line) {
    String data = line.substring(6);
    if (data.endsWith("}")) {
      OpenAiChatResponseVo chatResponse = FastJson2Utils.parse(data, OpenAiChatResponseVo.class);
      List<Choice> choices = chatResponse.getChoices();
      if (choices.size() > 0) {
        ChatResponseDelta delta = choices.get(0).getDelta();
        String part = delta.getContent();
        if (part != null && part.length() > 0) {
          completionContent.append(part);
          MaxKbStreamChatVo maxKbStreamChatVo = new MaxKbStreamChatVo();
          maxKbStreamChatVo.setContent(part);
          maxKbStreamChatVo.setChat_id(chatId).setId(messageId);
          maxKbStreamChatVo.setOperate(true).setIs_end(false);

          String message = JsonUtils.toJson(maxKbStreamChatVo);
          SseEmitter.pushSSEChunk(channelContext, message);
        }
      }
    } else {
      // [done]
      MaxKbStreamChatVo maxKbStreamChatVo = new MaxKbStreamChatVo();
      maxKbStreamChatVo.setContent("");
      maxKbStreamChatVo.setChat_id(chatId).setId(messageId);
      maxKbStreamChatVo.setOperate(true).setIs_end(true);
      String message = JsonUtils.toJson(maxKbStreamChatVo);
      SseEmitter.pushSSEChunk(channelContext, message);
      log.info("data not end with }:{}", line);
    }
  }

  private String functionCall(StringBuffer fnCallName, StringBuffer fnCallArgs) {
    // 实现你的 functionCall 逻辑
    return null;
  }

  public void cleanup(Long chatId) {
    ChatStreamCallCan.remove(chatId);
  }
}
