FROM litongjava/jre:8u391-stable-slim

# 设置工作目录
WORKDIR /app

# 复制 jar 文件到容器中
COPY target/java-maxkb-1.0.0.jar /app/

# 运行 jar 文件
CMD ["java", "-jar", "java-maxkb-1.0.0.jar", "--app.env=prod"]