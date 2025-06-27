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

# Git 브랜치 전략

## 이슈 기반 개발 워크플로우

1. **이슈 생성**: GitHub에서 새로운 이슈를 생성합니다
2. **브랜치 생성**: 이슈 번호를 포함한 브랜치를 생성합니다
3. **작업 수행**: 해당 브랜치에서 이슈 내용에 맞는 작업을 진행합니다
4. **PR 생성**: develop 브랜치로 Pull Request를 생성합니다

## 브랜치 명명 규칙

```bash
# 기본 형태
{타입}/#이슈번호-간단한-설명

# 브랜치 타입별 예시
feature/#190-ranking-refactor-to-layered    # 새로운 기능 개발
refactor/#189-user-pet-layered-architecture # 리팩토링
docs/#194-git-branch-strategy-docs          # 문서화 작업
fix/#xxx-bug-fix-description                # 버그 수정
chore/#xxx-dependency-update                # 빌드, 의존성 등
```

### 브랜치 타입 가이드
- **feature/**: 새로운 기능 개발
- **refactor/**: 코드 리팩토링
- **docs/**: 문서화 작업 (README, CLAUDE.md 등)
- **fix/**: 버그 수정
- **chore/**: 빌드 스크립트, 의존성 업데이트 등

## 주요 명령어

```bash
# 현재 이슈 목록 확인
gh issue list

# 특정 이슈 상세 정보 확인
gh issue view 190

# 새 브랜치 생성 및 체크아웃
git checkout -b feature/#190-ranking-refactor-to-layered

# develop 브랜치로 PR 생성
gh pr create --title "refactor: Ranking 기능 레이어드 아키텍처로 변경 (#190)" --body "$(cat <<'EOF'
## Summary
- UseCase 로직을 Service 레이어로 통합
- Controller가 Service를 직접 호출하도록 수정
- Gateway 인터페이스 제거 및 Repository 직접 의존으로 변경

## Changes
- `GetRanking`, `GetRankingBoard`, `GetTopRanking`, `UpdateRankingBoard` UseCase 제거
- `RankingService`에 해당 로직 통합
- `RankingController`가 `RankingService` 직접 호출
- `RankingBoardGateway` 인터페이스 제거
- `RankingService`가 `RankingBoardRepository` 직접 의존

## Test plan
- [ ] 기존 테스트 케이스 통과 확인
- [ ] API 동작 정상 확인
- [ ] 빌드 및 배포 정상 확인

Closes #190
EOF
)"
```

## PR 본문 템플릿

PR을 생성할 때는 다음 형식을 따릅니다:

```markdown
## Summary
- 주요 변경사항을 3-5개 bullet point로 요약

## Changes
- 구체적인 코드 변경 내용
- 추가/수정/삭제된 파일들
- 주요 로직 변경사항

## Test plan
- [ ] 단위 테스트 통과 확인
- [ ] 통합 테스트 확인  
- [ ] API 동작 테스트
- [ ] 빌드 및 배포 확인

Closes #이슈번호
```

# 개발 시 참고사항

- MongoDB는 `@Document` 어노테이션을 사용한 엔티티로 관리
- JWT 토큰은 `JWTTokenProvider`에서 생성/검증
- 예외 처리는 `WebExceptionHandler`에서 글로벌 처리
- 모든 API는 Swagger UI로 문서화됨 (`/swagger-ui.html`)
- 로깅은 `LoggingFilter`와 `LoggingUtils`를 활용

# Commit & PR 가이드

## PR 제목 가이드
- PR 제목을 작성할 때 아직 마지막에 (#199) 처럼 이슈 번호를 작성하지 마