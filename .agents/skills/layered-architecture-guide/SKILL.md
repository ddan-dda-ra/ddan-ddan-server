---
name: layered-architecture-guide
description: ddan-ddan-server의 layered 아키텍처 의존 규칙과 패키지 구조 가이드. domain/application/infrastructure/presentation 4개 레이어의 책임, 의존 방향, Gateway 패턴(폐기 예정) 처리 방침을 다룬다. 새 코드 추가, 리팩토링, 코드 리뷰 시 반드시 참조할 것 — 레이어 의존 위반은 가장 흔하고 가장 치명적인 결함이다.
---

# Layered Architecture Guide (ddan-ddan-server)

## 패키지 루트

```
notbe.tmtm.ddanddanserver
├── domain/         (가장 안쪽, 다른 레이어를 모름)
├── application/    (domain에만 의존)
├── infrastructure/ (domain에 의존, 외부 시스템 연결)
└── presentation/   (application에 의존, 외부 노출)
```

## 의존 방향 (단방향)

```
presentation ──► application ──► domain ◄── infrastructure
```

**도메인은 누구도 import하지 않는다.** application과 infrastructure가 도메인을 사용한다. presentation은 application까지만 안다.

### 절대 위반

- ❌ `domain` → 그 외 어떤 레이어든 import
- ❌ `application` → `presentation`, `infrastructure` (단, 인터페이스 의존은 OK — 아래 Gateway 항목 참조)
- ❌ `presentation` → `infrastructure`, `domain` 직접 참조 (DTO ↔ 도메인 변환은 application 또는 매퍼에서)

## 레이어별 책임

### domain/

| 디렉토리 | 책임 |
|---|---|
| `model/` | 비즈니스 도메인 객체 — 풍부 모델, 비즈니스 규칙은 여기 |
| `exception/` | 도메인별 커스텀 예외 (예: `CheerAlreadyExistsException`) |
| `gateway/` | (폐기 예정) 외부 의존에 대한 인터페이스 — 새 코드는 가급적 사용 금지 |

**규칙:**
- Spring 어노테이션 금지 (`@Service`, `@Component`, `@Document` 등 모두 X)
- MongoDB ObjectId만 예외적으로 도메인에서 사용 (현재 컨벤션)
- 데이터 클래스 우선, 행위는 도메인 메서드로

### application/

| 디렉토리 | 책임 |
|---|---|
| `service/` | 유스케이스 단위의 조정 로직 (`@Service`) |
| `processor/` | OAuth 등 Factory 패턴이 필요한 처리기 |

**규칙:**
- `@Service` + 생성자 주입 + `val`
- 인프라 객체는 인터페이스로 받아 구현체와 분리 (예: `CheerRepository`, `PushClient`)
- 트랜잭션 경계는 service에 위치
- 컨트롤러 DTO를 직접 받지 않음 (받더라도 DTO → 도메인 변환은 컨트롤러 또는 매퍼에서)

### infrastructure/

| 디렉토리 | 책임 |
|---|---|
| `database/` | MongoDB 엔티티(`@Document`), Repository |
| `api/` | 외부 API 클라이언트 (Kakao, Apple, Slack) |
| `client/` | 내부 정의된 클라이언트 추상화 (예: `PushClient`) |

**규칙:**
- `@Document` 엔티티는 **반드시** infrastructure에만 둔다
- 도메인 모델 ↔ 엔티티 변환 매퍼를 둔다 (Repository 구현체에서)
- application의 Repository **인터페이스**를 구현하는 형태가 일반적

### presentation/

| 디렉토리 | 책임 |
|---|---|
| `controller/` | REST API (`@RestController`) |
| `dto/` | 요청/응답 DTO (record/data class) |
| `filter/` | JWT 인증, 로깅, 버전 필터 |
| `scheduler/` | `@Scheduled` 정기 작업 |

**규칙:**
- 컨트롤러는 service 호출 + DTO 변환만 담당. 비즈니스 로직 금지.
- OpenAPI 어노테이션(`@Operation`, `@Parameter`, `@ApiResponse`) 필수
- DTO ↔ 도메인 변환은 컨트롤러 또는 별도 매퍼

## Gateway 패턴 처리 (폐기 예정)

`domain/gateway/`와 `infrastructure/gateway/`는 향후 제거 대상이다 (AGENTS.md 명시).

**새 코드 작성 시:**
- domain에 인터페이스를 두지 말고, **application/service에서 인프라 인터페이스(Repository, Client)에 의존**
- 인프라 측에서 그 인터페이스를 구현
- 즉, "포트는 application 옆에, 어댑터는 infrastructure에"

**기존 Gateway 코드 수정 시:**
- 동작은 유지하되, 새 의존을 추가하지 않는다
- 리팩토링 PR에서 함께 정리

## 모듈(기능) 단위 패키지

각 기능은 레이어 안에서 별도 하위 패키지로 분리된다:

```
domain/model/cheer/        ← Cheer 도메인 모델
application/service/CheerService.kt
infrastructure/database/repository/CheerRepository.kt
presentation/controller/CheerController.kt
presentation/dto/cheer/    ← cheer 관련 DTO
```

**새 기능 추가 시 체크리스트:**
- [ ] `domain/model/{feature}/`에 도메인 모델
- [ ] `domain/exception/`에 커스텀 예외 (필요 시)
- [ ] `application/service/{Feature}Service.kt`
- [ ] `infrastructure/database/`에 Document + Repository (필요 시)
- [ ] `presentation/controller/{Feature}Controller.kt`
- [ ] `presentation/dto/{feature}/`에 요청/응답 DTO

## 자주 만나는 위반과 권고

| 위반 | 권고 |
|---|---|
| service가 `@Document` 엔티티를 그대로 반환 | Repository에서 도메인 모델로 변환해 반환 |
| 컨트롤러에서 비즈니스 분기 | service 메서드로 이동 |
| 도메인 객체에 `@JsonProperty` | DTO를 별도로 두고 매핑 |
| 도메인 예외를 컨트롤러에서 try/catch | `WebExceptionHandler`에 매핑 추가 |
| application/service가 다른 service의 컨트롤러를 호출 | service 간 직접 호출 또는 도메인 이벤트 |
