FROM litongjava/jdk:25_0_2-stable-slim

# 设置工作目录
WORKDIR /app

# 安装Chromium浏览器
RUN apt update && apt install chromium -y && rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/*


COPY secrets.txt /app/secrets.txt
COPY target/maxkb-web-1.0.4.jar /app/

# 运行 jar 文件
CMD ["java", "-jar", "maxkb-web-1.0.4.jar", "--app.env=prod"]