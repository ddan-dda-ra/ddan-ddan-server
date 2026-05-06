# 리뷰 결과

## 요약
- ✅ 승인 가능 — Critical/Major 없음. Minor 권고 2건만 선택 적용.

## Critical (반드시 수정)
- 없음.

## Major (수정 권장)
- 없음.

## Minor (선택)
- [ ] **`registeredAt` 타임존 표기** — `src/main/kotlin/notbe/tmtm/ddanddanserver/application/event/UserRegisteredEventListener.kt:26` — `event.registeredAt`은 `Instant.toString()`으로 UTC ISO-8601(`2026-05-02T10:15:30Z`)이 그대로 찍힌다. 운영팀이 채널을 한국 시간 기준으로 본다면 `DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Seoul"))`로 포맷하는 것을 권고. 04_test_summary.md의 test-engineer 의견과 동일. 본 PR 범위에서는 사용자 결정에 따라 그대로 두어도 무방.
- [ ] **`SlackHookApi.username` 하드코딩과의 비대칭** — `src/main/kotlin/notbe/tmtm/ddanddanserver/infrastructure/api/DiscordHookApi.kt:21` — `DiscordHookApi`는 `DEFAULT_USERNAME` 컨스턴트로 노출했지만 기존 `SlackHookApi.kt:18`은 같은 값을 인라인 문자열로 들고 있다. 공용 상수(예: `infrastructure/api/HookConstants.kt`)로 빼는 것이 일관성 측면에서 좋지만, 본 PR 범위 외 정리이므로 후속 리팩토링 PR에서 처리 권장.
- [ ] **`SlackHookApi`에서 `username` 필드도 인라인이지만 `SlackHookApi.Request.username` 기본값 패턴은 그대로** — 위와 동일. 무리한 본 PR 확대 금지.

## 칭찬
- **설계서 준수도 매우 높음.** 02_design.md의 "왜 이 조합인가" 근거(트랜잭션 커밋 후 발송 + 누적 카운트 정확성 + 응답 지연 방지)가 그대로 코드에 녹아 있다.
- **트랜잭션 안전성 확보.** `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` + `@Async` 조합으로 (a) 가입 트랜잭션 롤백 시 알림 미발송, (b) listener 예외가 가입 흐름에 역류하지 않음, (c) webhook 지연이 OAuth 응답 지연으로 이어지지 않음을 모두 보장. `DdanDdanServerApplication.kt:12`의 `@EnableAsync`가 이미 활성화돼 있어 별도 설정 없이 동작.
- **메인 흐름 보호 견고함.** `UserRegisteredEventListener.kt:21-37`의 단일 `try { ... } catch (e: Exception)` 블록이 webhook IO/HTTP/직렬화 모든 예외를 흡수. 운영 알림 누락은 ERROR 로그로 사후 추적 가능.
- **PII 로깅 컨벤션 준수.** `logger().error(...)` 호출(line 31-36)에 `userId`/`oAuthType`만 남기고 `nickName`은 일부러 제외. kotlin-spring-conventions의 "민감 정보 로깅 금지" 원칙 정확히 지킴. Discord payload에는 그대로 노출되지만 이는 사용자 결정(02_design.md 결정 이유 항).
- **이벤트 객체가 plain data class.** `UserRegisteredEvent.kt`에 Spring 어노테이션이 일절 없어 application 레이어 내부 매개체로서의 위치가 깔끔. 도메인 모델로 끌고 가지 않은 것도 적절(side-effect용 데이터).
- **테스트 커버리지 양호.** Kotest FunSpec + MockK + 한국어 테스트명 + `beforeEach` 패턴 정확. `slot`으로 페이로드 캡처 후 `shouldContain`으로 메시지 형식 검증, `shouldNotThrow`로 예외 흡수 검증, `verify(exactly = 0/1)`로 분기 검증 — 한 가지 결함도 없이 정도(正道)로 작성됨.
- **AuthService 테스트의 신규/기존 분기 검증.** `eventPublisher.publishEvent(any<UserRegisteredEvent>())`를 신규 가입에서만 1회, 기존 사용자에서 0회 검증한 것이 회귀 방지에 효과적.
- **YAML 들여쓰기 일관성.** 03_implementation.md에서 명시한 대로 작업 지시("2-space")보다 기존 파일 컨벤션(4-space)을 우선해 일관성 유지. 적절한 판단.

## 환경/보안 점검
- **webhook URL 하드코딩 없음** — 세 yaml 모두 `${DISCORD_WEBHOOK_URL}` 환경변수만 참조. 코드 상수에는 어떤 webhook URL도 박혀 있지 않음.
- **트랜잭션 phase 검증 — AFTER_COMMIT 사용 확인됨** (`UserRegisteredEventListener.kt:19`).
- **메인 가입 흐름 차단 위험 없음** — listener의 try/catch + `@Async` 조합으로 webhook 실패가 OAuth login API에 영향을 줄 경로 없음.
- **layered 의존 방향** — `UserRegisteredEventListener`가 `infrastructure.database.repository.UserRepository`(인터페이스, `interface UserRepository : MongoRepository`)와 `infrastructure.api.DiscordHookApi`(인터페이스)를 import. layered-architecture-guide 가이드에서 허용한 "application/service에서 인프라 인터페이스(Repository, Client)에 의존" 패턴 준수. AuthService 등 기존 service들과 동일 패턴.

## implementer에게 전달
- Critical/Major 없음. **수정 없이 그대로 PR 생성 가능.**
- Minor 2건은 모두 본 PR 범위를 벗어나는 후속 정리 권장 사항이므로, 별도 이슈/PR로 분리하거나 본 PR에서는 무시해도 무방.
- PR 메시지 한국어 컨벤션 (`feat: 신규 가입 디스코드 알림 추가` 등)으로 작성 권장. 이슈 번호가 있다면 본문 끝에 `close #이슈번호`.
