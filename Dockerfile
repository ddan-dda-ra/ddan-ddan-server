# 1. 빌드 스테이지
FROM gradle:8.8-jdk17 as builder

# 작업 디렉토리 설정
WORKDIR /app

# Gradle 파일만 먼저 복사하여 캐시를 효율적으로 활용
COPY build.gradle.kts settings.gradle.kts /app/

# 의존성 설치 (테스트 제외)
RUN gradle build -x test --no-daemon

# 소스 코드 복사
COPY src /app/src

# Gradle 빌드
RUN gradle build -x test --no-daemon

# 2. 실행 스테이지 (Alpine 기반으로 경량화)
FROM openjdk:17-alpine

# 필수 패키지 설치 (Alpine에서 호환성 문제 해결을 위한 경우)
RUN apk add --no-cache bash

# 작업 디렉토리 설정
WORKDIR /app

# builder 스테이지에서 빌드된 jar 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 애플리케이션 실행 명령어
ENTRYPOINT ["java", "-jar", "-Duser.timezone=Asia/Seoul", "app.jar"]