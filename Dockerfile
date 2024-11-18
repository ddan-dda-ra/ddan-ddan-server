# 1. 빌드 스테이지
FROM gradle:8.8-jdk17 as builder

# 작업 디렉토리 설정
WORKDIR /app

# 필요한 파일들 복사
COPY build.gradle.kts settings.gradle.kts /app/
COPY src /app/src

# Gradle 빌드
RUN gradle build -x test --no-daemon

# 2. 실행 스테이지
FROM openjdk:17-jdk-slim

# 작업 디렉토리 설정
WORKDIR /app

# builder 스테이지에서 빌드된 jar 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 애플리케이션 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]
