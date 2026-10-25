# 使用官方的 OpenJDK 17 镜像作为基础镜像
FROM openjdk:17-jdk-slim
# 设置容器内的工作目录
WORKDIR /app
# 将本地的 jar 包复制到容器中并重命名为 app.jar
# 注意：请将 your-application-name.jar 替换为你实际打包出的 jar 包名
COPY target/Charity-0.0.1-SNAPSHOT.jar app.jar
# 声明容器对外暴露的端口（与你的 application.yml 中 server.port 一致）
EXPOSE 8080
# 容器启动时执行的命令
ENTRYPOINT ["java", "-jar", "app.jar"]