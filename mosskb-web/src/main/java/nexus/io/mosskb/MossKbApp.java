package nexus.io.mosskb;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import nexus.io.annotation.AComponentScan;
import nexus.io.mosskb.config.boot.MossKbBootConfig;
import nexus.io.tio.boot.TioApplication;
import nexus.io.tio.boot.server.TioBootServer;

@AComponentScan("nexus.io.mosskb.controller")
public class MossKbApp {
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

    MossKbBootConfig mossKbBootConfig = new MossKbBootConfig();
    TioApplication.run(MossKbApp.class, mossKbBootConfig, args);
    long end = System.currentTimeMillis();
    System.out.println((end - start) + "ms");
  }
}