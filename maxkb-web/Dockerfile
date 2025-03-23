FROM litongjava/jre:8u391-stable-slim

# 设置工作目录
WORKDIR /app

# 安装Chromium浏览器
RUN apt update && apt install chromium -y && rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/*

# 复制 jar 文件到容器中
COPY target/java-maxkb-1.0.0.jar /app/

# 运行 jar 文件
CMD ["java", "-jar", "java-maxkb-1.0.0.jar", "--app.env=prod"]