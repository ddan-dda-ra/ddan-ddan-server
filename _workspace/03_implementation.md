# 구현 결과

## 변경 파일

| 파일 | 동작 | 요약 |
|---|---|---|
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/event/UserRegisteredEvent.kt` | 생성 | Spring 무관 plain `data class`. 필드: `userId(ObjectId)`, `nickName(String)`, `oAuthType(OAuthType)`, `registeredAt(Instant)`. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/event/UserRegisteredEventListener.kt` | 생성 | `@Component`, `@Async + @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`. `userRepository.count()` → `[PROFILE] 신규 가입 ...` 한 줄 메시지 조립 → `discordHookApi.sendMessage(...)`. `try/catch (Exception)`로 모든 예외 흡수, `logger().error`에 `userId`/`oAuthType`만 로깅 (nickName 미포함). 천단위 콤마는 `String.format("%,d", count)`. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/infrastructure/api/DiscordHookApi.kt` | 생성 | `SlackHookApi`와 동일한 패턴(`@HttpExchange` + `@PostExchange` + `data class Request` + `companion object`). Discord webhook 스펙에 맞춰 payload 키는 `content`, `username`. `DEFAULT_USERNAME = "ddan-ddan-server-bot"`. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/infrastructure/api/ApiConfiguration.kt` | 수정 | `slackHookApi` 빈 직후 동일한 형태로 `discordHookApi` `@Bean` 추가. `@Value("\${discord.hook-url}")` + 기존 `restClientAdapter(baseUrl)` 헬퍼 재사용. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/service/AuthService.kt` | 수정 | 생성자 마지막 파라미터로 `private val eventPublisher: ApplicationEventPublisher` 추가. `loginNewUser()` 내부 `authRepository.save(...)` 직후 `return AuthResult` 직전에 `eventPublisher.publishEvent(UserRegisteredEvent(..., registeredAt = Instant.now()))` 1줄 추가. import 2건 추가(`ApplicationEventPublisher`, `Instant`, `UserRegisteredEvent`). 기존 메서드 시그니처 변동 없음. |
| `src/main/resources/application-prod.yaml` | 수정 | `slack:` 블록 직후에 `discord:` 블록 추가 (`hook-url: ${DISCORD_WEBHOOK_URL}`). 들여쓰기는 기존 파일과 동일한 4-space. |
| `src/main/resources/application-dev.yaml` | 수정 | 동상. |
| `src/main/resources/application-local.yaml` | 수정 | 동상. |

## 핵심 결정

- **yaml 들여쓰기**: 작업 지시는 "2-space"였으나, 기존 세 yaml 파일 모두 최상위 키 하위에 4-space를 사용 중(`slack:` 블록 포함). 컨벤션 일관성을 위해 기존 파일과 동일한 4-space로 작성. 기존 컨벤션을 깨지 않기 위한 결정.
- **그 외 설계 대비 변경 사항 없음.** 설계서 그대로 구현.

## 빌드 검증

- `./gradlew compileKotlin`: 통과. (로컬 기본 JDK가 25로 잡혀 있어 Gradle 8.8 호환성 이슈로 1차 실패 → JDK 17(`/usr/libexec/java_home -v 17.0.14`)로 재시도해 `BUILD SUCCESSFUL`. 본 변경과 무관한 환경 이슈이며, 코드 자체는 문제 없음.)
- 출력된 경고는 모두 기존 코드의 Kotlin KT-73255(어노테이션 타깃 변경) 마이그레이션 경고로 본 작업과 무관.

## test-engineer에게 전달할 메모

- **`UserRegisteredEventListener` 단위 테스트 시 mock 포인트**:
  - `userRepository.count()` — `every { userRepository.count() } returns 1234L`
  - `discordHookApi.sendMessage(any())` — `every { discordHookApi.sendMessage(any()) } just runs` 또는 예외 던지는 케이스
  - `@Value("\${spring.profiles.active:local}") activeProfile` — 생성자 인자로 직접 문자열 주입(`"prod"`, `"dev"`, `"local"`) 가능
- **검증 포인트**:
  - 메시지 본문 정확성: `[PROD] 신규 가입 | 닉네임=홍길동 | provider=KAKAO | userId=... | 가입시각=... | 누적 가입자=1,234명` 형식. 천단위 콤마 확인.
  - `discordHookApi.sendMessage`가 정확히 1회 호출되는지 (`verify(exactly = 1) { ... }`)
  - `discordHookApi.sendMessage`가 예외를 던져도 `handle()`에서 throw되지 않는지(예외 흡수 검증)
  - 예외 발생 시 logger error 호출은 직접 검증보다 "throw되지 않음"으로 검증하는 편이 견고
- **`AuthService` 단위 테스트**:
  - 생성자에 `ApplicationEventPublisher` 추가됨 → 기존 `AuthService` 테스트의 mock 생성자에 `eventPublisher` 인자 추가 필요. mockk이라면 `mockk<ApplicationEventPublisher>(relaxed = true)`로 받고, 신규 가입 케이스에 한해 `verify { eventPublisher.publishEvent(any<UserRegisteredEvent>()) }`로 발행 1회 검증.
  - `loginExistUser()` 경로에서는 이벤트가 발행되지 않아야 함 → `verify(exactly = 0) { eventPublisher.publishEvent(any<UserRegisteredEvent>()) }`.
  - `Instant.now()`는 publish 직전에 호출되므로 정확한 값 비교는 `match { it.userId == ... && it.oAuthType == ... }` 식 부분 매칭 권장.
- **트랜잭션 / `@Async` 행위 검증**: 단위 테스트로는 잡기 어렵다. `@TransactionalEventListener(AFTER_COMMIT)`/`@Async` 동작은 통합 테스트 영역. 단위 테스트는 "이벤트 발행"과 "리스너 핸들러 동작"을 분리해서 검증하면 충분.
- **외부 의존**: Discord webhook은 실제 호출되지 않도록 `DiscordHookApi`를 항상 mock으로 주입할 것. 통합 테스트에서도 webhook URL을 더미로 두면 `RestClient`가 실제 네트워크 호출을 시도하지 않게 막아야 함(또는 `DiscordHookApi`를 `@MockBean`).
