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

현재 ktlint Gradle 플러그인은 주석 처리되어 있습니다 (`build.gradle.kts`의 `org.jlleitschuh.gradle.ktlint`). 린트는 PR에서 GitHub Actions(`ktlint-check.yml`, ScaCap/action-ktlint)가 수행하며, 로컬 린트 명령은 없습니다(미검증). `.editorconfig`: 들여쓰기 4칸, 최대 120자, LF, 파일 끝 개행.

# 기술 스택

- **언어**: Kotlin 2.2.0 (JVM 17 toolchain)
- **프레임워크**: Spring Boot 3.5.4
- **데이터베이스**: MongoDB (Spring Data MongoDB)
- **인증**: JWT + OAuth2 (Kakao, Apple)
- **푸시 알림**: Firebase FCM
- **문서화**: SpringDoc OpenAPI (Swagger)
- **모니터링**: Spring Actuator + Prometheus
- **컨테이너**: Docker (Jib 플러그인)
- **테스트**: JUnit 5 + MockK

# 아키텍처

- layered 아키텍처로 구성되어 있다.

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
    - 이슈 제목은 간단명료하게 작성합니다
    - 이슈 내용에 작업의 목적과 세부사항을 기재합니다
    - 이슈 번호는 브랜치 명명 규칙에 사용됩니다
    - 이슈 제목의 형식은 `[작업 형태] 간단한 설명`입니다
    - 작업 형태는 `feat`, `refactor`, `docs`, `fix`, `chore` 등으로 구분합니다
2. **브랜치 생성**: 이슈 번호를 포함한 브랜치를 생성합니다
   - 브랜치 이름은 `타입/#이슈번호-간단한-설명` 형식으로 작성합니다
3. **작업 수행**: 해당 브랜치에서 이슈 내용에 맞는 작업을 진행합니다
4. **PR 생성**: develop 브랜치로 Pull Request를 생성합니다
    - PR 제목은 `타입: 설명` 형식으로 작성합니다
    - PR 제목에 끝에 이슈번호는 추가하지 않습니다
    - PR 본문은 `.github/pull_request_template.md` 템플릿을 사용합니다

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
## 🔎 작업 내용
-

## ➕ 기타
-

close #이슈번호
EOF
)"
```

# 개발 시 참고사항

- MongoDB는 `@Document` 어노테이션을 사용한 엔티티로 관리
- JWT 토큰은 `JWTTokenProvider`에서 생성/검증
- 예외 처리는 `WebExceptionHandler`에서 글로벌 처리
- 모든 API는 Swagger UI로 문서화됨 (`/swagger-ui.html`)
- 클라이언트용 API 문서 진입점은 루트의 `API.md`이며, 상세 문서는 `docs/api/`에 도메인별로 관리합니다.
- 정확한 HTTP 계약은 `docs/api/openapi.yaml`, 업무 규칙과 호출 흐름은 `docs/api/*.md`를 기준으로 합니다.
- controller, DTO, 인증, 상태 코드 또는 에러 코드 변경 시 같은 작업에서 API 문서를 반드시 갱신합니다.
- OpenAPI 스냅샷은 서버 실행 후 `./scripts/update-openapi.sh [base-url]`로 갱신합니다.
- API 문서 변경 후 `./scripts/validate-api-docs.sh`를 실행합니다.
- 로깅은 `LoggingFilter`와 `LoggingUtils`를 활용
- 파일 끝에는 항상 개행 문자를 추가합니다

# 프로젝트 규칙 (에이전트 작업용)

> `AGENTS.md`는 이 파일을 가리키는 심볼릭 링크다. 원본은 `CLAUDE.md` 하나.
> 아래 "검증"은 2026-10-30 기준 이 worktree에서 확인한 결과다.

## 1. 빌드·테스트·린트와 완료 기준

| 용도 | 명령 | 비고 |
|---|---|---|
| 빌드 | `./gradlew build` | 미검증 |
| 테스트 | `./gradlew test` | CI(`pr-test.yml`, develop 대상 PR)가 `./gradlew test --continue --info`로 실행. **로컬 실행 전 아래 주의 확인. 미검증** |
| 특정 테스트 | `./gradlew test --tests "*클래스명"` | 위 주의 동일 |
| 린트 | (로컬 명령 없음) | PR에서 `ktlint-check.yml`이 실행 |
| API 문서 검증 | `./scripts/validate-api-docs.sh` | ruby 필요. 이 Mac에서는 ruby가 `libgmp.10.dylib` 누락으로 실행 실패(환경 문제) → 미검증 |
| 이미지 빌드/푸시 | `./gradlew jib` | Docker Hub 푸시는 CI(`docker-build-push.yml`)가 담당. 로컬 실행 금지 |

**테스트 주의 (운영 DB 보호)**
- 이 Mac은 홈서버를 겸하며 `127.0.0.1:27017`은 **운영 MongoDB**다.
- 리포지토리 통합 테스트(`*RepositoryIntegrationTest`, `AuthUniqueIndexInitializerIntegrationTest` 등)는 `@EnabledIfEnvironmentVariable(RUN_MONGO_IT=true)`일 때만 실행되고, 기본 접속 대상이 `${MONGO_IT_HOST:localhost}:${MONGO_IT_PORT:27017}`, DB `ddan-ddan-it-test`이며 테스트 전후로 컬렉션을 비운다. **`RUN_MONGO_IT`를 설정하지 않는다.** 필요하면 `MONGO_IT_PORT`를 운영이 아닌 별도 Mongo로 지정해야 하며, 그런 인스턴스가 없으면 실행하지 않는다.
- `@SpringBootTest` 풀 컨텍스트 테스트(`DdanDdanServerApplicationTests`, `migration/*`, `TestUserGenerator`)는 `@Disabled`다. 이 파일들을 활성화하지 않는다(실제 DB 마이그레이션 코드 포함).
- 임베디드 Mongo/Testcontainers 의존은 없다. 나머지 테스트는 MockK/`@WebMvcTest` 기반으로 보이나, `./gradlew test` 전체가 27017에 접속하지 않는다는 것은 실행으로 확인하지 못했다(미검증). 에이전트는 `RUN_MONGO_IT` 미설정 상태에서 대상 테스트 클래스를 하나씩 지정해 실행하고, DB가 필요한 클래스는 건너뛴다.
- `src/main/resources/config`는 git submodule(`ddan-ddan-server-config`, 비공개 설정)이며 이 worktree에서는 비어 있다. 열거나 채우거나 출력하지 않는다.

**완료로 인정하는 기준**
- 변경한 코드에 대응하는 단위 테스트를 추가/수정하고, 영향받는 테스트 클래스가 통과(위 주의를 지켜 실행한 출력 첨부).
- Spring 어노테이션(`@Transactional`, `@TransactionalEventListener`, `@Async` 등)에 의존하는 동작은 단위 테스트만으로 부족하다. 컨텍스트 기반 테스트(`@WebMvcTest` 등)로 검증한다.
- controller/DTO/인증/상태·에러 코드를 바꿨다면 `API.md`·`docs/api/*`·`docs/api/openapi.yaml`을 같은 PR에서 갱신하고 `validate-api-docs.sh` 통과(실행 불가한 환경이면 그 사실을 보고).
- 컴파일(`./gradlew build` 또는 `compileKotlin compileTestKotlin`)이 통과한다.

## 2. 로컬 실행·포트·setup

- 실행: `./gradlew bootRun`. 기본 프로필은 `application.yaml`의 `spring.profiles.active: dev`이고 `application-{dev,local,prod}.yaml`이 존재한다. 세 프로필 모두 MongoDB 접속 정보를 환경변수(`MONGO_HOST`, `MONGO_PORT`, `MONGO_DB`, `MONGO_USER`, 비밀번호 등)로 받는다. 기본값이 없어 setup 없이는 기동하지 않는다.
- 필요 setup: JDK 17(`mise`로 관리), `git submodule update --init`으로 `src/main/resources/config` 받기(접근 권한 필요), 위 환경변수. 외부 연동(Kakao/Apple OAuth, Firebase FCM, Slack)은 설정/자격 증명이 필요하다. `mock-oauth.enabled`로 OAuth를 mock할 수 있다(기본 `false`).
- 포트: 앱은 8080(Jib `ports`, Swagger UI `/swagger-ui.html`, OpenAPI `/v3/api-docs.yaml`).
- **이 Mac에서 `bootRun` 금지 사항**: 8080은 홈서버 dev/prod 앱이 컨테이너로 쓰는 포트와 겹칠 수 있고 `127.0.0.1:27017`은 운영 MongoDB다. 운영 DB를 가리키는 환경변수로 서버를 띄우지 않는다. 로컬 실행이 필요하면 `--server.port`로 다른 포트(예: 18080)를 지정하고 운영이 아닌 Mongo를 사용한다(미검증).
- 배포 이미지: `amazoncorretto:17-alpine-jdk`, linux/arm64, 타임존 `Asia/Seoul`, 이미지 태그는 `GITHUB_SHA`(없으면 `dev`).

## 3. 건드리면 안 되는 영역과 브랜치/커밋/PR 규칙

**건드리지 않는 영역**
- `src/main/resources/config/` (비공개 설정 submodule), `.env`·인증서·키·토큰류.
- `src/test/.../migration/*`의 `@Disabled` 마이그레이션 코드는 운영 데이터를 변경할 수 있다. 실행·활성화 금지.
- `.github/workflows/*`(배포 파이프라인)는 사용자 요청 없이 수정하지 않는다. 최근 커밋에서 GitOps 태그 갱신 단계를 제거한 이력이 있다(#352).
- MongoDB 필드 매핑은 `SnakeCaseFieldNamingStrategy`(엔티티 camelCase ↔ DB snake_case)다. 쿼리/마이그레이션 키는 snake_case로 쓴다.
- 운영 DB 쓰기, 컨테이너 재기동은 사용자 확인 없이 하지 않는다.

**브랜치/커밋/PR** (아래 "Git 브랜치 전략" 절이 상세 원본)
- 기본 브랜치는 `develop`, PR 대상도 `develop`. 이슈 기반 `타입/#이슈번호-간단한-설명` 브랜치.
- 커밋/PR 제목은 `타입: 설명` 형식의 한국어(예: `fix: ... (#350)`). PR 본문은 `.github/pull_request_template.md`.
- `CODEOWNERS`는 `@ddingmin` 한 명. CodeRabbit 자동 리뷰가 develop 대상 PR에 켜져 있다(한국어).
- Orca 워커는 push·PR 생성을 하지 않고 로컬 커밋까지만 한다. 이슈/브랜치 번호가 없는 지시면 코디네이터가 준 브랜치를 그대로 쓴다.

## 4. 작업 분할 경계

- 패키지 루트: `src/main/kotlin/notbe/tmtm/ddanddanserver/` 아래 `domain/`, `application/`, `infrastructure/`, `presentation/`, `common/`, `config/` (위 "레이어 구조" 참조). 단일 Gradle 모듈이라 모듈 경계는 없다.
- 분할 기준은 **레이어가 아니라 기능(auth/user/pet/ranking/notification 등) 단위의 수직 슬라이스**를 권장한다. 한 기능이 controller→service→repository를 함께 건드리기 때문이다.
- 동시에 한 파일을 두 워커가 맡지 않게 한다. 충돌 가능성이 높은 공유 파일: `build.gradle.kts`, `application*.yaml`, `WebExceptionHandler`, `WebSecurityConfig`, `docs/api/openapi.yaml`, `API.md`.
- API 문서(`docs/api/*`)는 해당 기능을 바꾸는 워커가 같이 소유한다. `openapi.yaml`은 서버 실행으로 생성되므로 수동 병합 충돌을 피하려고 한 워커가 마지막에 갱신한다.
- `scripts/`(Node `pet-catalog-assets.mjs`와 테스트, ruby 검증 스크립트)와 `docs/`는 서버 코드와 독립적으로 분할할 수 있다.
