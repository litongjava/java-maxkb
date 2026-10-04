package nexus.io.mosskb.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecutorServiceUtils {

  private static final int CONCURRENT_REQUESTS = 100;
  private static final ExecutorService executorService = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);

  /**
   * 文档解析专用执行器。
   *
   * <p>解析一份大文档要几分钟且几乎全在等远程 OCR，用每任务一个虚拟线程的线程池，避免占用固定池的
   * 100 个工作线程，也避免多份大文档互相顶掉。
   */
  private static final ExecutorService documentParseExecutor =
      Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("doc-parse-", 0).factory());

  public static ExecutorService getExecutorService() {
    return executorService;
  }

  public static ExecutorService getDocumentParseExecutor() {
    return documentParseExecutor;
  }
}
