package com.litongjava.maxkb.enumeration;

public enum ModelType {

  EMBEDDING("EMBEDDING"), LLM("LLM");

  private String name;

  // 构造函数
  ModelType(String name) {
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
