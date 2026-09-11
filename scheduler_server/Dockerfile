# syntax=docker/dockerfile:1

# --- build stage -----------------------------------------------------------
FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

# 의존성 레이어를 먼저 굳혀 소스만 바뀔 때 재다운로드를 피한다.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# --- runtime stage ---------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy AS runtime

# root 로 돌리지 않는다.
RUN groupadd --system banking && useradd --system --gid banking --create-home banking

WORKDIR /app
COPY --from=build --chown=banking:banking /workspace/build/libs/*.jar /app/scheduler_server.jar

USER banking

ENV TZ=Asia/Seoul \
    SERVER_PORT=8181 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

EXPOSE 8181

# API_BASE_URL / INTERNAL_API_KEY 는 이미지에 굽지 않고 실행 시점에 주입한다.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Duser.timezone=$TZ -jar /app/scheduler_server.jar"]
