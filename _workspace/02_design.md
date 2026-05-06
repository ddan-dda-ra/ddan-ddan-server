# 설계: 신규 가입 디스코드 알림

## 도메인 모델

없음. Discord 운영 알림은 비즈니스 도메인 개념이 아니라 운영팀 통지 부수효과(side-effect)이므로 `domain/model/`에 새 모델을 만들지 않는다. 이벤트 객체(`UserRegisteredEvent`)는 application 레이어 내부 통신 매개체이며 도메인 모델이 아니다.

## 이벤트

### `UserRegisteredEvent` (application/event)

`AuthService.loginNewUser()` 트랜잭션이 **커밋된 이후** Discord 발송에 필요한 모든 정보를 옮기는 불변 데이터 클래스.

**필드:**

| 필드 | 타입 | 의미 / 출처 |
|---|---|---|
| `userId` | `org.bson.types.ObjectId` | `newUser.id` — Discord 메시지에서 식별자 표기에 사용 |
| `nickName` | `String` | `oAuth.nickName` — Discord 메시지의 사용자명 표기 (사용자 결정: 마스킹 없이 그대로 노출, Apple의 이메일 노출 위험은 사용자가 감수) |
| `oAuthType` | `notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType` | `KAKAO` / `APPLE` — provider 표기 |
| `registeredAt` | `java.time.Instant` | 이벤트 생성 시각(가입 시각). 트랜잭션 커밋 후 시각이 아니라 신규 User 생성 시점을 보존하기 위해 publish 직전에 `Instant.now()`로 채운다 |

**필드 선정 근거:**
- Discord 메시지 본문 구성에 필요한 최소 필드만 담는다. `User` 객체 전체나 `Auth` 객체 전체를 넣지 않는 이유는 (1) listener에서 도메인 메서드를 호출할 일이 없고 (2) 이벤트 객체는 영속 객체 참조를 들고 트랜잭션 경계를 넘기지 않는 편이 안전하기 때문이다(LazyInitialization 등 우회 가능 이슈 회피 + 의도 명확화).
- `누적 가입자 수`는 이벤트 필드에 넣지 않는다. listener가 `AFTER_COMMIT` 시점에 `userRepository.count()`로 직접 조회한다. 이렇게 해야 신규 사용자 자신을 포함한 정확한 카운트가 나온다.

## 서비스 시그니처

### `AuthService` 변경 (application/service/AuthService.kt 수정)

생성자에 `ApplicationEventPublisher`를 주입받고, `loginNewUser()` 내부에서 `userRepository.save(...)`/`authRepository.save(...)` 직후 이벤트를 발행한다.

```kotlin
@Service
class AuthService(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JWTTokenProvider,
    private val oauthProcessorFactory: OAuthProcessorFactory,
    private val eventPublisher: org.springframework.context.ApplicationEventPublisher, // 추가
) {
    // ...

    private fun loginNewUser(
        oAuth: OAuth,
        deviceToken: String?,
        oAuthType: OAuthType,
    ): AuthResult {
        val newUser = userRepository.save(
            User.register(name = oAuth.nickName, deviceToken = deviceToken),
        )
        authRepository.save(
            Auth.create(oAuthId = oAuth.id, type = oAuthType, userId = newUser.id),
        )

        // 추가: 트랜잭션 커밋 후 Discord 알림 발송을 위한 이벤트 발행
        eventPublisher.publishEvent(
            UserRegisteredEvent(
                userId = newUser.id,
                nickName = oAuth.nickName,
                oAuthType = oAuthType,
                registeredAt = Instant.now(),
            ),
        )

        return AuthResult(
            accessToken = jwtTokenProvider.createAccessToken(newUser),
            refreshToken = jwtTokenProvider.createRefreshToken(newUser),
            user = newUser,
        )
    }
}
```

**기존 메서드 시그니처(반환 타입, 파라미터) 변경 없음.** 컨트롤러/외부 API 영향 없음.

### `UserRegisteredEventListener` (신규: application/event/UserRegisteredEventListener.kt)

```kotlin
@Component
class UserRegisteredEventListener(
    private val userRepository: UserRepository,
    private val discordHookApi: DiscordHookApi,
    @Value("\${spring.profiles.active:local}") private val activeProfile: String,
) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: UserRegisteredEvent)
}
```

**핸들러 책임:**
1. `userRepository.count()`로 누적 가입자 수 조회 (트랜잭션 커밋 후이므로 신규 사용자 포함됨).
2. `activeProfile.uppercase()`로 phase 접두어 생성 (`PROD`, `DEV`, `LOCAL`).
3. 한국어 한 줄 메시지 본문 조립.
4. `discordHookApi.sendMessage(...)`를 `try { ... } catch (e: Exception) { log.error(...) }`로 감싸 모든 예외 흡수.

**메시지 포맷 (예시):**

```
[PROD] 신규 가입 | 닉네임=홍길동 | provider=KAKAO | userId=66341a2b3c4d5e6f78901234 | 가입시각=2026-05-02T14:23:11Z | 누적 가입자=1,234명
```

- 단일 `content` 한 줄 문자열. Discord embed 미사용 (사용자 결정 가이드: "content 한 줄도 충분").
- 천단위 콤마는 `String.format("%,d", count)` 사용.

## 외부 클라이언트

### `DiscordHookApi` (신규: infrastructure/api/DiscordHookApi.kt)

`SlackHookApi`를 1:1 미러링한다. `@HttpExchange` + `@PostExchange` + `RestClient` baseUrl 주입 패턴 동일.

```kotlin
@HttpExchange(accept = [MediaType.APPLICATION_JSON_VALUE])
interface DiscordHookApi {
    @PostExchange
    fun sendMessage(
        @RequestBody request: Request,
    )

    data class Request(
        val content: String,
        val username: String = DEFAULT_USERNAME,
    )

    companion object {
        const val DEFAULT_USERNAME = "ddan-ddan-server-bot"
    }
}
```

**Slack과의 차이 (Discord webhook 스펙 준수):**
- payload 키는 `content`, `username` (Slack은 `text`, `channel`, `username`).
- Discord webhook은 채널을 URL 자체에 묶기 때문에 `channel` 필드가 없다.
- `avatar_url`, `embeds` 등 추가 필드는 본 설계 범위에서 사용하지 않음 (단순 한 줄 알림으로 충분).

### `ApiConfiguration` 수정 (infrastructure/api/ApiConfiguration.kt)

`slackHookApi` 빈과 동일한 형태로 `discordHookApi` 빈 추가. `@Value("\${discord.hook-url}")`로 webhook URL 주입.

```kotlin
@Bean
fun discordHookApi(
    @Value("\${discord.hook-url}") hookUrl: String,
): DiscordHookApi {
    val factory = HttpServiceProxyFactory.builderFor(restClientAdapter(hookUrl)).build()
    return factory.createClient(DiscordHookApi::class.java)
}
```

기존 `restClientAdapter(baseUrl: String)` 헬퍼 그대로 재사용. 신규 헬퍼 함수 불필요.

## API 명세

**외부 API 변경 없음.** presentation 레이어 무변화. 기존 `/auth/login` 시리즈 엔드포인트의 요청/응답 시그니처도 그대로다. 본 기능은 OAuth 가입 흐름의 부수효과로만 동작한다.

## MongoDB 스키마

**변경 없음.** `users` 컬렉션의 `count()`만 읽으며, 신규 컬렉션/필드/인덱스 추가 없음.

## 예외

**신규 도메인 예외 없음.** Discord 알림 실패는 비즈니스 실패가 아니라 운영 통지 부수효과 실패이므로 `WebExceptionHandler`에 매핑할 필요가 없다.

`UserRegisteredEventListener.handle()`은 단일 `try { ... } catch (e: Exception) { log.error(...) }` 블록으로 모든 예외(IO, HTTP 4xx/5xx, 직렬화 등)를 흡수한다. 재시도 없음 (사용자 결정: fire-and-forget).

```kotlin
private val log = LoggerFactory.getLogger(javaClass)

try {
    discordHookApi.sendMessage(DiscordHookApi.Request(content = body))
} catch (e: Exception) {
    log.error("Discord 신규 가입 알림 발송 실패: userId={}, provider={}", event.userId, event.oAuthType, e)
}
```

로그에는 사용자 PII(`nickName`)를 남기지 않는다 — `userId`와 provider만. (kotlin-spring-conventions 가이드 준수: "민감 정보는 절대 로깅 금지".)

## 환경변수 / 설정

### `discord.hook-url` 추가

`slack.hook-url`과 동일한 컨벤션 (yaml 키 kebab-case, 환경변수 UPPER_SNAKE).

| 파일 | 추가 라인 |
|---|---|
| `src/main/resources/application-prod.yaml` | `discord:`<br>` hook-url: ${DISCORD_WEBHOOK_URL}` |
| `src/main/resources/application-dev.yaml` | `discord:`<br>` hook-url: ${DISCORD_WEBHOOK_URL}` |
| `src/main/resources/application-local.yaml` | `discord:`<br>` hook-url: ${DISCORD_WEBHOOK_URL}` |

세 yaml 모두 `slack:` 블록 바로 아래(혹은 인접한 외부 통합 영역)에 들여쓰기 2-space로 추가한다. 단일 webhook URL을 공유하되 메시지 본문에 phase 접두어를 붙여 채널 분리 효과를 낸다 (사용자 결정).

### `spring.profiles.active` 사용처

`UserRegisteredEventListener` 생성자에서 `@Value("\${spring.profiles.active:local}") private val activeProfile: String`로 주입받아 메시지 prefix에 사용. 기본값 `local`로 두어 profile 미설정 환경에서도 안전.

### `@Async` 동작 보장

`DdanDdanServerApplication`에 `@EnableAsync`가 이미 활성화되어 있어(이슈 분석 메모 확인) 별도 설정 불필요. `@TransactionalEventListener`는 spring-tx에 포함되어 추가 의존성 없음.

## 변경 파일 목록

### 생성

- `src/main/kotlin/notbe/tmtm/ddanddanserver/application/event/UserRegisteredEvent.kt`
- `src/main/kotlin/notbe/tmtm/ddanddanserver/application/event/UserRegisteredEventListener.kt`
- `src/main/kotlin/notbe/tmtm/ddanddanserver/infrastructure/api/DiscordHookApi.kt`

### 수정

- `src/main/kotlin/notbe/tmtm/ddanddanserver/application/service/AuthService.kt` — 생성자에 `ApplicationEventPublisher` 추가, `loginNewUser()` 끝(return 직전)에서 `eventPublisher.publishEvent(UserRegisteredEvent(...))` 1줄 추가
- `src/main/kotlin/notbe/tmtm/ddanddanserver/infrastructure/api/ApiConfiguration.kt` — `discordHookApi` `@Bean` 메서드 추가
- `src/main/resources/application-prod.yaml` — `discord.hook-url` 항목 추가
- `src/main/resources/application-dev.yaml` — `discord.hook-url` 항목 추가
- `src/main/resources/application-local.yaml` — `discord.hook-url` 항목 추가

테스트 파일은 test-engineer가 별도로 추가한다.

## 결정 이유

### 왜 `ApplicationEventPublisher` + `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` 조합인가

1. **트랜잭션 커밋 후 발송 보장.** `AuthService.login()`은 `@Transactional`이 걸려 있어 신규 사용자/Auth 저장이 트랜잭션 안에서 일어난다. 만약 service 내부에서 webhook을 직접 호출하면 (a) 호출 중 예외가 던져져 트랜잭션이 롤백되거나 (b) 커밋 전에 알림이 나가버리는 문제가 생긴다. `@TransactionalEventListener(phase = AFTER_COMMIT)`은 정확히 이 두 문제를 함께 막는다 — 커밋이 완료된 뒤에만 호출되고, listener에서 던진 예외는 원래 트랜잭션에 영향을 주지 않는다.
2. **누적 가입자 수의 정확성.** `userRepository.count()`를 listener에서 호출하면 새로 저장된 사용자 자신이 카운트에 포함된 결과가 나온다. service 내부에서 호출하면 트랜잭션 격리에 따라 카운트에 자기 자신이 빠질 수 있고 (read-uncommitted가 아닌 한), 트랜잭션 경계 안에서 호출되어 의도가 흐려진다.
3. **메인 흐름과의 분리 (응답 지연 방지).** `@Async`로 별도 쓰레드에서 실행하므로 Discord webhook이 수 초 지연되어도 OAuth 로그인 응답은 즉시 반환된다. `@EnableAsync`가 이미 적용돼 있어 추가 부트스트랩 비용 없음.
4. **대안 비교.**
   - "service 내부에서 직접 webhook 호출 + `@Async`만 적용": 트랜잭션 미커밋 상태에서 알림이 나갈 수 있음. **탈락.**
   - "controller에서 service 호출 후 webhook 호출": controller가 비즈니스 부수효과를 책임지게 되어 layered 의존 규칙(presentation→application 단방향) 안에 두기 어렵고, controller에 신규/기존 분기를 다시 만들어야 함. **탈락.**
   - "도메인 이벤트를 domain 모델에 부착": Spring `ApplicationEvent`는 domain 모델을 Spring에 묶는 결과가 되어 layered-architecture-guide의 "domain은 Spring 어노테이션 금지" 원칙을 위반. **탈락.**
   - 따라서 **이벤트 객체는 application 레이어 plain class**로 두고, publisher/listener도 application에 위치시키는 것이 가장 깔끔하다.

### 왜 재시도가 없는가 (사용자 결정)

- 사용자 결정: 운영 알림 1건 누락보다는 코드 단순성과 외부 의존도 최소화를 우선. fire-and-forget으로 처리하고 실패는 `logger.error`로 흔적만 남긴다. 운영 부재 시 ELK/CloudWatch 등에서 ERROR 로그를 검색해 누락 가입을 사후 보정 가능.
- 기존 `RetryConfig`의 `RetryTemplate`을 끌어다 쓸 수 있지만 본 설계에서는 사용하지 않는다. 재시도 정책이 추후 필요해지면 listener 안에서만 추가하면 되므로 확장 용이.

### 왜 phase 접두어로 채널을 통합하는가 (사용자 결정)

- 사용자 결정: dev/prod webhook URL을 별도로 두면 Vault/CI 환경변수 관리 부담이 두 배가 된다. 단일 webhook URL을 유지하되 메시지 첫머리에 `[PROD]` / `[DEV]` / `[LOCAL]` 접두어를 붙여 운영팀이 시각적으로 구분.
- 트레이드오프: dev 노이즈가 같은 채널에 흘러들어가지만, phase 접두어로 운영팀이 필터링/검색 가능. dev 트래픽이 과도해지면 추후 이 설계를 깨지 않고 yaml만 분리해 channel webhook을 환경별로 다르게 줄 수 있다.

### 왜 nickName을 그대로 노출하는가 (사용자 결정)

- 사용자 결정: Apple 로그인 시 `OAuth.nickName`이 이메일이라는 것을 알고 있으나, 운영팀 전용 Discord 채널이고 마스킹 로직 추가 시 KakaoProcessor 닉네임도 함께 가독성이 떨어지는 트레이드오프 발생. 사용자가 PII 노출 위험을 감수하기로 결정.
- 보완: 로그(`logger.error`)에는 `nickName`을 남기지 않는다 — 코드 컨벤션 가이드의 "민감 정보 로깅 금지" 원칙은 준수. Discord webhook payload는 운영팀에 한정된 1회성 통보이므로 영구 저장 위험은 webhook 수신 시스템(Discord) 보존 정책에 위임.
