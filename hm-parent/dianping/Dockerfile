# 使用 OpenJDK 的基础镜像
FROM openjdk:11.0-jre-buster

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone


COPY dianping-0.0.1-SNAPSHOT.jar /app.jar


ENTRYPOINT ["java", "-jar", "/app.jar"]
