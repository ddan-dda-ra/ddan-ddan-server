# 테스트 결과

## 추가된 테스트 파일

- `src/test/kotlin/notbe/tmtm/ddanddanserver/application/event/UserRegisteredEventListenerTest.kt` — 6개 테스트 (정상 4 / 예외 2)
- `src/test/kotlin/notbe/tmtm/ddanddanserver/application/service/AuthServiceTest.kt` — 3개 테스트 (정상 3 / 예외 0)
  - 기존 `AuthServiceTest`는 존재하지 않아 신규 작성. 기존 케이스 회귀 우려 없음.

## 커버한 케이스

### UserRegisteredEventListenerTest
- 신규 가입 이벤트 수신 시 디스코드 웹훅에 정확한 페이로드로 발송한다
  - 메시지 형식 검증: `[PROD] 신규 가입`, `닉네임=...`, `provider=KAKAO`, `userId=...`, `가입시각=...`, `누적 가입자=1,234명`
  - `userRepository.count()`가 정확히 1회 호출되는지 검증
  - `discordHookApi.sendMessage`가 정확히 1회 호출되는지 검증
- activeProfile이 소문자여도 메시지 접두어는 대문자로 변환된다 (`dev` → `[DEV]`, `provider=APPLE` 검증)
- 누적 가입자 수가 천 단위 이상이면 콤마가 포함된다 (1,234,567명)
- 디스코드 웹훅 호출이 예외를 던져도 리스너는 예외를 흡수한다 (`shouldNotThrow`)
- userRepository.count() 호출이 예외를 던져도 리스너는 예외를 흡수한다 (`shouldNotThrow` + `discordHookApi.sendMessage`가 호출되지 않음 검증)
- 디스코드 요청의 username 필드는 기본값을 따른다 (`DiscordHookApi.DEFAULT_USERNAME`)

### AuthServiceTest
- 신규 사용자 가입 시 UserRegisteredEvent가 발행된다
  - `eventPublisher.publishEvent(any<UserRegisteredEvent>())` 정확히 1회
  - 발행된 이벤트의 `nickName`, `oAuthType`, `userId` 필드가 신규 유저와 일치
- 기존 사용자 로그인 시 UserRegisteredEvent가 발행되지 않는다 (`verify(exactly = 0)`)
- 기존 사용자가 동일한 deviceToken으로 로그인하면 사용자 저장이 호출되지 않는다 (분기 검증, 이벤트 미발행도 함께 확인)

## 실행 결과

```
./gradlew test --tests "notbe.tmtm.ddanddanserver.application.event.*" \
              --tests "notbe.tmtm.ddanddanserver.application.service.AuthServiceTest"
```

- BUILD SUCCESSFUL
- UserRegisteredEventListenerTest: 6 tests, 0 failures, 0 errors (0.585s)
- AuthServiceTest: 3 tests, 0 failures, 0 errors (0.197s)
- 합계: ✅ 9/9 통과

JDK는 환경 안내대로 `JAVA_HOME=$(/usr/libexec/java_home -v 17.0.14)`로 실행.

## implementer에게 전달할 피드백

- **테스트 용이성: 양호.** 인계 메모대로 `UserRegisteredEventListener`의 `activeProfile`을 `@Value` 주입이지만 생성자 인자로 그대로 직접 주입할 수 있어 mock 없이 phase 분기 검증이 깔끔했음.
- **`AuthService`도 양호.** `ApplicationEventPublisher`를 생성자에 노출했기 때문에 `mockk(relaxed = true)`로 받고 `slot`/`verify(exactly = 0/1)`만으로 분기를 충분히 검증 가능.
- **개선 제안 (선택):** `UserRegisteredEventListener.handle()`의 `Instant.now()`는 `AuthService.loginNewUser()`에서 호출되는데, 만약 후속에 메시지 본문이 한국 시간 포맷팅을 요구하게 되면 `Clock` 주입을 고려해 볼 만함. 현재 범위에서는 불필요.
- **`@Async`/`@TransactionalEventListener(AFTER_COMMIT)` 검증은 통합 테스트 영역**으로 분리됨 (단위 테스트 범위 외). 본 PR에서는 작성 안 함.
- **`DiscordHookApi`/`ApiConfiguration` 단위 테스트 미작성** — 인터페이스 + Spring 빈 정의로 단위 테스트 가치가 낮아 작업 지시대로 생략.
