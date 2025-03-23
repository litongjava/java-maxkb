package com.litongjava.maxkb.enumeration;

public enum ModelProvider {
  model_openai_provider("model_openai_provider");

  private String name;

  // 构造函数
  ModelProvider(String name) {
    this.name = name;
  }

  // 获取描述信息
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return this.name();
  }
}
