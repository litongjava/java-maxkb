package com.litongjava.maxkb.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Call;

public class ChatStreamCallCan {
  public static Map<Long, okhttp3.Call> callMap = new ConcurrentHashMap<>();

  public static okhttp3.Call stop(Long id) {
    Call call = callMap.get(id);
    if (call != null && !call.isCanceled()) {
      //call.cancel();
      return callMap.remove(id);
    }
    return null;
  }

  public static okhttp3.Call remove(Long id) {
    return callMap.remove(id);
  }

  public static void put(Long chatId, Call call) {
    callMap.put(chatId, call);
  }
}
