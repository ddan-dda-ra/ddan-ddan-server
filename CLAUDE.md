# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# 프로젝트 개요

Kotlin과 Spring Boot 기반의 다마고치 게임 백엔드 서버입니다. 사용자는 OAuth 로그인을 통해 동물을 키우고, 칼로리를 관리하며, 다른 사용자들과 랭킹을 비교할 수 있습니다.

# 개발 명령어

## 빌드 및 실행
```bash
# 프로젝트 빌드
./gradlew build

# 애플리케이션 실행
./gradlew bootRun

# 테스트 실행
./gradlew test

# Docker 이미지 빌드
./gradlew jib
```

## 코드 품질
현재 ktlint는 주석 처리되어 있습니다 (build.gradle.kts:4).

# 기술 스택

- **언어**: Kotlin 1.9.24
- **프레임워크**: Spring Boot 3.3.1
- **데이터베이스**: MongoDB (Spring Data MongoDB)
- **인증**: JWT + OAuth2 (Kakao, Apple)
- **푸시 알림**: Firebase FCM
- **문서화**: SpringDoc OpenAPI (Swagger)
- **모니터링**: Spring Actuator + Prometheus
- **컨테이너**: Docker (Jib 플러그인)
- **테스트**: JUnit 5 + MockK

# 아키텍처

현재 Clean Architecture 패턴을 기반으로 구성되어 있으며, 최종적으로는 useCase, Gateway 패턴을 제거하여 layered 아키텍처로 리팩토링 예정입니다.

## 레이어 구조

### Domain Layer (`domain/`)
- **model/**: 비즈니스 도메인 모델 (User, Pet, Auth, Ranking 등)
- **exception/**: 도메인별 커스텀 예외 클래스
- **gateway/**: 외부 의존성에 대한 인터페이스 (향후 제거 예정)

### Application Layer (`application/`)
- **service/**: 비즈니스 로직을 처리하는 서비스 클래스
- **processor/**: OAuth 처리를 위한 프로세서 (Factory 패턴)

### Infrastructure Layer (`infrastructure/`)
- **database/**: MongoDB 엔티티 및 리포지토리
- **api/**: 외부 API 클라이언트 (Kakao, Apple, Slack)
- **gateway/**: Gateway 인터페이스 구현체 (향후 제거 예정)

### Presentation Layer (`presentation/`)
- **controller/**: REST API 엔드포인트
- **dto/**: 요청/응답 데이터 전송 객체
- **filter/**: JWT 인증 및 로깅 필터
- **scheduler/**: 정기 실행 스케줄러

# 주요 기능

## 인증 (`auth/`)
- OAuth2 로그인 (Kakao, Apple)
- JWT 토큰 기반 인증/인가
- 토큰 갱신

## 사용자 관리 (`user/`)
- 사용자 정보 관리
- 사용자 설정 (알림, 개인정보)
- 메인 펫 설정

## 반려동물 (`pet/`)
- 반려동물 추가/조회
- 먹이 주기, 놀아주기
- 랜덤 펫 추가

## 랭킹 시스템 (`ranking/`)
- 사용자 랭킹 조회
- 랭킹 보드 관리
- 주기별 랭킹 업데이트

## 푸시 알림 (`notification/`)
- FCM을 통한 푸시 알림
- 칼로리 체크 알림
- 주간 랭킹 알림
- 비활성 사용자 알림

# 설정 파일

- `application.yaml`: 메인 설정 파일
- `config/application.yaml`: 환경별 설정

# Docker 배포

Jib 플러그인을 사용하여 Docker 이미지를 빌드합니다:
- 베이스 이미지: amazoncorretto:17-alpine-jdk
- 플랫폼: linux/arm64
- 포트: 8080
- 타임존: Asia/Seoul

# 개발 시 참고사항

- MongoDB는 `@Document` 어노테이션을 사용한 엔티티로 관리
- JWT 토큰은 `JWTTokenProvider`에서 생성/검증
- 예외 처리는 `WebExceptionHandler`에서 글로벌 처리
- 모든 API는 Swagger UI로 문서화됨 (`/swagger-ui.html`)
- 로깅은 `LoggingFilter`와 `LoggingUtils`를 활용
