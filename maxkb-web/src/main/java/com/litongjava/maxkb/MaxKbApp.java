package com.litongjava.maxkb;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.litongjava.annotation.AComponentScan;
import com.litongjava.tio.boot.TioApplication;
import com.litongjava.tio.boot.server.TioBootServer;

@AComponentScan("com.litongjava.maxkb.controller")
public class MaxKbApp {
  public static void main(String[] args) {
    long start = System.currentTimeMillis();

    // 1. 虚拟线程工厂（用于 work 线程）
    ThreadFactory workTf = Thread.ofVirtual().name("t-io-v-", 1).factory();

    // 2. 创建业务虚拟线程 Executor（每任务一个虚拟线程）
    ThreadFactory bizTf = Thread.ofVirtual().name("t-biz-v-", 0).factory();

    ExecutorService bizExecutor = Executors.newThreadPerTaskExecutor(bizTf);

    // 3. 设置 readWorkers 和 Executor 使用的线程工厂
    TioBootServer server = TioBootServer.me();
    server.setWorkThreadFactory(workTf);
    server.setBizExecutor(bizExecutor);

    TioApplication.run(MaxKbApp.class, args);
    long end = System.currentTimeMillis();
    System.out.println((end - start) + "ms");
  }
}