package com.litongjava.maxkb;

import com.litongjava.annotation.AComponentScan;
import com.litongjava.hotswap.watcher.HotSwapResolver;
import com.litongjava.tio.boot.TioApplication;

@AComponentScan
public class MaxKbApp {
  public static void main(String[] args) {
    long start = System.currentTimeMillis();
    HotSwapResolver.addSystemClassPrefix("com.litongjava.maxkb.vo.");
    //    TioApplicationWrapper.run(MaxKbApp.class, args);
    TioApplication.run(MaxKbApp.class, args);
    long end = System.currentTimeMillis();
    System.out.println((end - start) + "ms");
  }
}