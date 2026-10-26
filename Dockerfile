# 使用官方的 OpenJDK 17 镜像作为基础镜像
FROM openjdk:17-jdk-slim
# 设置容器内的工作目录
WORKDIR /app
# 将本地的 jar 包复制到容器中并重命名为 app.jar
# 注意：请将 your-application-name.jar 替换为你实际打包出的 jar 包名
COPY target/Charity-0.0.1-SNAPSHOT.jar app.jar
# 声明容器对外暴露的端口（与你的 application.yml 中 server.port 一致）
EXPOSE 8080
# EasyExcel(底层 POI) 导出 Excel 时需要字体度量库，slim 镜像默认不带，
# 否则导出会抛 UnsatisfiedLinkError: libfreetype.so.6: cannot open shared object file
# 只保留主源：bullseye 官方镜像的 security 源部分包已 404，直接用会 install 失败
RUN echo 'deb http://deb.debian.org/debian bullseye main' > /etc/apt/sources.list \
    && rm -f /etc/apt/sources.list.d/* \
    && apt-get update \
    && apt-get install -y --no-install-recommends libfreetype6 fontconfig fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

# 时区：容器默认 UTC，LocalDateTime.now() 会比北京时间慢 8 小时。
# TZ 影响系统时间，user.timezone 直接用 JDK 自带时区库兜底（不依赖镜像里有没有 tzdata）
ENV TZ=Asia/Shanghai

# 容器启动时执行的命令
ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-jar", "app.jar"]