package com.litongjava.maxkb.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecutorServiceUtils {

  private static final int CONCURRENT_REQUESTS = 100;
  private static final ExecutorService executorService = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
  
  public static ExecutorService getExecutorService() {
    return executorService;
  }
}
