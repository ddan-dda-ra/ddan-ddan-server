# 이슈 분석: 신규 가입 디스코드 알림

## 요구사항 요약

OAuth(Kakao/Apple) 로그인 흐름에서 **신규 사용자가 처음 생성되는 시점**에 운영팀의 Discord 관리자 채널로 자동 알림을 보낸다. 발송 주체는 서버이며 사용자 행위로 직접 트리거되지 않는다. Discord 연결은 Webhook URL 방식(Bot Token 아님)을 사용하고, URL은 `application.yaml`에 환경변수로 주입한다. 알림 페이로드 후보는 사용자명, OAuth provider(kakao/apple), 가입 시각, 누적 가입자 수다. 기존 사용자 로그인은 알림 대상이 아니다. Webhook 호출 실패가 가입 흐름을 차단해서는 안 된다.

## 영향 받는 레이어

- [ ] domain
- [x] application
- [x] infrastructure
- [ ] presentation
- [x] config (application.yaml)

신규 도메인 모델은 필요 없음. 알림은 운영팀용 외부 통보일 뿐 비즈니스 도메인 개념이 아니므로 `infrastructure/api`에 Discord HTTP 클라이언트를, `application/service`(또는 `AuthService` 내부)에서 호출 지점을 둔다. presentation 레이어는 변경 없음 (기존 `/auth` 엔드포인트의 응답 시그니처는 유지).

## 관련 코드

| 파일 | 라인 | 역할 |
|---|---|---|
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/service/AuthService.kt` | 24-35 | `login()` — OAuth 로그인 진입점. `authRepository.findByOAuthIdAndType` 결과로 신규/기존 분기. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/service/AuthService.kt` | 50-68 | `loginNewUser()` — **신규 가입 분기**. `userRepository.save(...)`로 User 생성 후 `authRepository.save(...)`로 Auth 생성. 알림 호출은 이 메서드의 마지막(저장 성공 직후, return 직전)에 위치해야 한다. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/processor/OAuth.kt` | 5-9 | OAuth DTO. `id`, `type: OAuthType`, `nickName` 보유 — 알림 페이로드 소스. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/domain/model/auth/OAuthType.kt` | 3-6 | `KAKAO`, `APPLE` enum. provider 표기에 사용. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/domain/model/user/User.kt` | 76-81 | `User.register()` 팩토리. 신규 User 식별자/이름이 여기서 만들어진다. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/infrastructure/api/SlackHookApi.kt` | 1-25 | **참조 모델**. `@HttpExchange` + `@PostExchange` 인터페이스, `Request(channel, text, username)` payload, `companion object`에 채널 상수. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/infrastructure/api/ApiConfiguration.kt` | 13-19 | **참조 모델**. `@Bean`으로 `RestClient` baseUrl을 webhook URL로 셋업하고 `HttpServiceProxyFactory`로 인터페이스 프록시 생성. `@Value("\${slack.hook-url}")` 패턴. |
| `src/main/resources/application-prod.yaml` | 24-25 | `slack.hook-url: ${SLACK_WEBHOOK_URL}` — 환경변수 명명 컨벤션 (kebab-case 키, UPPER_SNAKE 환경변수). dev/local 동일. |
| `src/main/resources/application-dev.yaml` | 24-25 | 동상. |
| `src/main/resources/application-local.yaml` | 24-25 | 동상. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/infrastructure/database/repository/UserRepository.kt` | 9 | `MongoRepository<User, ObjectId>` 상속 → `count(): Long` 자동 제공. **별도 메서드 추가 불필요.** |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/infrastructure/client/FirebasePushClient.kt` | 21, 35 | **참조 모델 (비동기/실패 격리)**. `@Async` 어노테이션 + `RetryTemplate`로 외부 호출 격리. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/DdanDdanServerApplication.kt` | 12 | `@EnableAsync` 이미 활성화 — `@Async` 즉시 사용 가능. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/config/RetryConfig.kt` | 7 | `RetryTemplate` 빈 정의 — Discord 호출에도 재사용 가능. |

## 참조할 기존 패턴

- **`SlackHookApi` (`infrastructure/api/SlackHookApi.kt` + `ApiConfiguration.kt`)** — Discord Webhook 클라이언트의 1:1 미러링 모델. 인터페이스 + `@HttpExchange` + `@PostExchange` + `RestClient` baseUrl 주입 방식, webhook URL을 `@Value`로 yaml에서 주입하는 패턴, 채널/봇이름 등 상수를 `companion object`로 모으는 컨벤션을 그대로 따른다. (단, Discord Webhook payload는 Slack과 키가 다르다 — Discord는 `content`, `username`, `avatar_url`, `embeds` 사용.)
- **`FirebasePushClient` (`infrastructure/client/FirebasePushClient.kt`)** — 외부 호출을 메인 흐름에서 떼어내는 패턴. `@Async` + `RetryTemplate.execute { ... }` 조합. Discord 알림도 동일 패턴이 적합하다 (가입은 사용자가 기다리는 동기 흐름이라 webhook 실패/지연이 절대 전이되면 안 된다).
- **`AuthService.loginNewUser` (`application/service/AuthService.kt:50-68`)** — 신규 분기 자체가 명확히 분리돼 있어, 알림 호출 1줄을 메서드 끝에 추가하면 된다. 별도 분기 식별 로직이 필요 없다.

## 위험 요소

- **메인 가입 흐름 차단** — Discord Webhook 호출이 동기적으로 수 초 블록되면 OAuth 로그인 응답이 늦어진다. 반드시 `@Async` 메서드로 위임하거나 `try/catch`로 예외를 흡수해야 한다. `FirebasePushClient` 패턴 차용 권장.
- **Webhook 실패 → 가입 실패 전이** — `@Transactional`(AuthService.login에 적용 중) 컨텍스트 안에서 webhook 호출 중 예외가 던져지면 user/auth 저장이 롤백된다. 두 가지 방어가 동시에 필요: (1) `@Async`로 트랜잭션 경계 밖으로 빼거나 (2) 호출부를 `try/catch`로 감싸 logger().error로만 남긴다. 추천: 둘 다.
- **트랜잭션 커밋 전 알림 발송** — `@Async`만 적용하면 별 쓰레드에서 즉시 실행돼 user 저장 트랜잭션이 아직 커밋되지 않았을 수 있다. `누적 가입자 수`를 알림에 포함하려면 커밋 후 `count()`가 신규 사용자를 포함하도록 해야 정확하다. → `ApplicationEventPublisher` + `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async` 조합이 가장 안전하다. 차선은 `AuthService.login()`이 끝난 뒤(=컨트롤러 레이어)에서 호출하는 것 (현재 controller는 미확인이므로 service 내부 이벤트 발행이 깔끔).
- **민감 정보 노출** — `OAuth.nickName`은 KakaoProcessor에서는 카카오 닉네임이지만 **AppleProcessor에서는 이메일을 그대로 담는다** (`AppleProcessor.kt:34` `nickName = claims["email"].toString()`). Discord 채널이 운영팀 전용이라도 이메일을 평문으로 보내는 것은 PII 정책상 위험. provider별 표기를 다르게 하거나 마스킹 필요 (예: `m***@kakao.com`). 또는 알림 본문에서 nickName 노출을 생략하고 `userId`만 표기.
- **환경변수 명명 일관성** — 기존 컨벤션은 `slack.hook-url: ${SLACK_WEBHOOK_URL}` (yaml 키는 kebab-case, 환경변수는 UPPER_SNAKE). Discord도 `discord.hook-url: ${DISCORD_WEBHOOK_URL}` 형식이 정합. dev/prod/local 세 yaml 모두 추가 필요.
- **누적 가입자 수 계산 비용** — `userRepository.count()`는 `users` 컬렉션 전체 카운트로, 데이터가 커지면 무시 못 할 비용이 된다. 가입은 빈도가 낮으므로 큰 문제는 아니지만, 알림에 포함을 강제할 만큼 가치가 있는지 한번 고려할 것.
- **dev/prod 채널 분리** — 운영팀 채널 하나로 두면 dev 환경 가입(테스트)도 같은 채널에 쏟아진다. profile별로 webhook URL을 다르게 주거나 dev에서는 알림을 끄는 토글이 권장된다.
- **`SlackHookApi`는 정의만 있고 실사용 코드 없음** — `grep -rn sendMessage` 결과 호출 지점이 없다. Discord 클라이언트를 같은 패턴으로 만들면 동일하게 "정의만 있고 안 쓰는 상태"가 되지 않도록 호출부(AuthService 또는 이벤트 리스너)까지 반드시 같이 구현해야 한다.

## 미해결 질문

- **닉네임 노출 정책** — Apple의 경우 `nickName`이 이메일이다. 그대로 보낼지, 마스킹할지, 아예 빼고 `userId`만 보낼지? (위험 요소의 PII 항목과 동일.)
- **dev/prod 채널 분리 여부** — 단일 webhook으로 통일할지, profile별로 다르게 둘지? 기본안은 단일이지만 dev 노이즈 우려가 있다.
- **알림 실패 시 재시도 정책** — `RetryTemplate`(기존 `RetryConfig`)을 재사용할지, "fire and forget"으로 끝낼지? 운영 알림은 한 번 놓치면 끝이라 재시도 1~2회 권장.
- **누적 가입자 수 포함 확정 여부** — 추측 기본안엔 들어 있으나 사용자 명시 동의는 없음. 비용/필요성 검토 필요.
