# 테스트 결과

## 추가/수정된 테스트 파일

| 파일 | 동작 | 케이스 수 | 정상/예외 |
|---|---|---|---|
| `src/test/kotlin/notbe/tmtm/ddanddanserver/application/processor/OAuthProcessorFactoryTest.kt` | 신규 | 8 | 정상 4 / 예외 4 |
| `src/test/kotlin/notbe/tmtm/ddanddanserver/application/service/AuthServiceTest.kt` | 보강 | 5 (+2) | 정상 5 / 예외 0 (기존 3 + 신규 2) |
| `src/test/kotlin/notbe/tmtm/ddanddanserver/common/WebExceptionHandlerTest.kt` | 보강 | 13 (+2) | 정상 13 / 예외 0 (기존 11 + 신규 2) |
| `src/test/kotlin/notbe/tmtm/ddanddanserver/presentation/controller/AuthControllerIntegrationTest.kt` | 신규 | 4 | 정상 3 / 예외 1 |

총 4개 파일에 12개 테스트 신규 작성, 기존 3개 테스트 시그니처 갱신.

## 커버한 케이스

### `OAuthProcessorFactoryTest` (신규, 8건)

- `real/mock processor가 함께 주입되면 마커 인터페이스 기준으로 정확히 분리된다`
- `useMock=false 요청 시 real processor가 반환된다`
- `useMock=true이고 mock processor가 존재하면 mock processor가 반환된다`
- `useMock=true인데 mockMap에 해당 타입이 없으면 UnsupportedOAuthModeException이 발생한다 (prod 시나리오)`
- `useMock=true && Apple mock 없음이면 data=APPLE로 UnsupportedOAuthModeException이 발생한다`
- `useMock=false인데 realMap에 해당 타입이 없으면 IllegalArgumentException이 발생한다`
- `useMock=false 요청에서는 mockMap에 있어도 real이 없으면 IllegalArgumentException이 발생한다 - mock 우회 불가`
- `mock processor만 주입되어도 partition은 동작하며 useMock=true로 조회 가능하다`

설계서 테스트 계획 1번 4가지 케이스를 모두 충족하고, 추가로 partition 견고성과 prod 시나리오에서의 `data` 페이로드까지 검증.

### `AuthServiceTest` (보강, 신규 2건)

기존 3건의 `getClient(OAuthType.KAKAO)` stubbing을 신규 시그니처 `getClient(OAuthType.KAKAO, false)`로 갱신. 추가로:

- `useMock=true가 전달되면 factory에 useMock=true로 전파된다`
- `useMock 미지정 시 default false로 factory에 전파된다`

각 케이스에서 `verify(exactly = 1) { factory.getClient(..., true|false) }` + `verify(exactly = 0)`로 반대 시그니처 호출이 없음까지 확인.

### `AuthControllerIntegrationTest` (신규, 4건)

`@WebMvcTest` + `@Import(TestConfig)` 패턴으로 `AuthController`만 격리해 띄움. `SecurityAutoConfiguration` 제외, `PetCatalogVersionFilter`가 글로벌이라 `PetCatalogService`도 빈으로 mock 제공.

- `X-Mock-OAuth 헤더가 없으면 useMock=false로 service에 전달된다` — `@RequestHeader(required=false, defaultValue="false")` 검증
- `X-Mock-OAuth=false 헤더가 들어오면 useMock=false로 service에 전달된다` — 명시적 false 바인딩
- `X-Mock-OAuth=true 헤더가 들어오면 useMock=true로 service에 전달된다` — Boolean true 바인딩 + service 호출 검증
- `service에서 UnsupportedOAuthModeException이 발생하면 400 응답과 AC006 에러코드를 반환한다` — `WebExceptionHandler` 매핑 통합 검증

slot 캡처로 controller→service 인자 전파를 정확히 검증. 마지막 케이스는 `mock-oauth.enabled=false` prod 시나리오를 service-level mocking으로 시뮬레이션한다 (mock 빈 미등록 상태에서 발생할 예외가 동일한 흐름이므로).

### `WebExceptionHandlerTest` (보강, 신규 2건)

- `UnsupportedOAuthModeException은 400 Bad Request로 처리되며 AC006 에러코드와 OAuthType data를 담는다`
- `UnsupportedOAuthModeException은 부모 AuthenticationException 401 핸들러보다 우선 매칭된다` — 구체 타입 핸들러 분리 의도 보존

기존 스타일(JUnit5)을 유지해 일관성 확보.

## 실행 결과

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew test
```

- 결과: BUILD SUCCESSFUL
- **전체 통계**: tests=270, skipped=0, failures=0, errors=0
- 본 PR 대상 4개 클래스: 0 실패
  - `OAuthProcessorFactoryTest` 8/8 통과
  - `AuthServiceTest` 5/5 통과
  - `WebExceptionHandlerTest` 13/13 통과
  - `AuthControllerIntegrationTest` 4/4 통과

### JDK 환경 주의

호스트 기본 `JAVA_HOME`이 JDK 25라 Kotlin 1.9.24 컴파일러(JavaVersion.parse("25") 미지원)와 비호환. 반드시 `JAVA_HOME=$(/usr/libexec/java_home -v 17)`로 명시 실행. 본 PR 변경과 무관한 환경 이슈.

## implementer에게 전달할 피드백

없음.

- 시그니처 변경(`useMock` 디폴트 false)으로 호환성을 잘 유지해 기존 테스트 깨짐이 최소화됨.
- `UnsupportedOAuthModeException`이 `data` 필드에 `OAuthType`를 정확히 담아주어 통합 테스트 검증이 깔끔했음.
- `OAuthProcessorFactoryTest` 작성 시 `@PostConstruct` 미호출 문제는 `init()` 직접 호출로 해결 — 설계서 메모대로 잘 작동.
- 통합 테스트에서 `PetCatalogVersionFilter`가 글로벌 필터라 `PetCatalogService` mock 빈을 추가로 등록해야 했지만, 기존 `UserAdminControllerIntegrationTest` 패턴 그대로 따름. 추후 글로벌 필터 의존이 더 늘면 공용 `TestConfig` base 추출을 고려할 만함 (이번 범위는 아님).

---

## 재실행 변경 이력

### 2026-05-22 — 리뷰 05_review.md Critical 2 + Major 1 + Major 2 대응

**범위:** Critical 2 (`mock-oauth.enabled=false` prod 시나리오 SpringBootTest 재작성) + Major 1 (`WebExceptionHandler` 매처 실증) + Major 2 (`@RequestHeader Boolean` invalid 값 실증) 통합 테스트 추가.

**추가/수정된 테스트 파일**

| 파일 | 동작 | 케이스 수 | 정상/예외 |
|---|---|---|---|
| `src/test/kotlin/notbe/tmtm/ddanddanserver/presentation/controller/AuthControllerProdModeIntegrationTest.kt` | 신규 | 6 | 정상 3 / 예외 3 |
| `src/test/kotlin/notbe/tmtm/ddanddanserver/presentation/controller/AuthControllerHeaderBindingIntegrationTest.kt` | 신규 | 8 | 정상 7 / 예외 1 |
| `src/test/kotlin/notbe/tmtm/ddanddanserver/presentation/controller/AuthControllerIntegrationTest.kt` | 수정 | 3 (−1) | 정상 3 / 예외 0 |

총 14개 신규 통합 테스트 추가, 1개 service-mock 기반 prod 시나리오 테스트 제거(대체됨).

**커버한 케이스 — `AuthControllerProdModeIntegrationTest` (6건)**

`@WebMvcTest` + `@Import` + `@TestPropertySource(["mock-oauth.enabled=false"])` 조합으로 `@ConditionalOnProperty` 평가 + 실제 `OAuthProcessorFactory.partition` + `WebExceptionHandler` 매처 평가를 통합 검증. **외부 카카오/Apple API는 mock**, 실제 processor 빈은 그대로 사용.

- `mock-oauth.enabled=false 환경에서 X-Mock-OAuth=true KAKAO 요청은 400 + AC006 + data=KAKAO를 반환한다 (Critical 2 + Major 1)` — `OAuthProcessorFactory.partition`이 mockMap을 비운 채 동작 → factory가 `UnsupportedOAuthModeException` throw → `WebExceptionHandler`가 자식 핸들러(400 AC006)를 부모 `AuthenticationException` 핸들러(401)보다 우선 매칭. Spring MVC dispatcher 전체 흐름 검증.
- `mock-oauth.enabled=false 환경에서 X-Mock-OAuth=true APPLE 요청은 400 + AC006 + data=APPLE을 반환한다` — APPLE 타입에 대해서도 동일 흐름 + `data` 페이로드 확인.
- `mock-oauth.enabled=false 환경에서도 OAuthProcessorFactory의 realMap에는 KAKAO와 APPLE이 모두 등록된다` — partition의 real 풀 보존 확인.
- `mock-oauth.enabled=false 환경에서 OAuthProcessorFactory_getClient(KAKAO, useMock=true)는 UnsupportedOAuthModeException을 직접 던진다` — factory 단위로도 mockMap 비어있음을 보증 (SpringContext 빈 등록 상태).
- `mock-oauth.enabled=false 환경에서 X-Mock-OAuth=false 헤더 + 외부 API 401 응답이면 OAuth 401 에러로 변환된다` — Real KakaoProcessor 분기 → KakaoUnauthorizedError → 401 KO003. 반대 분기(real) 확정.
- `mock-oauth.enabled=false 환경에서 헤더 없음 요청은 Real processor로 분기되어 외부 API를 호출한다` — `defaultValue="false"` 적용 확인.

`@SpringBootTest` 대신 `@WebMvcTest + @Import` 선택 이유: 이 코드베이스는 임베디드 MongoDB가 없어 `@SpringBootTest` 풀 컨텍스트가 MongoDB 부재로 부팅 실패. `@WebMvcTest + @Import`로 (a) MockMvc auto-config, (b) `@ConditionalOnProperty` 평가, (c) 실제 `OAuthProcessorFactory.partition` 동작, (d) `WebExceptionHandler` 매처 평가를 모두 만족 — `@AutoConfigureMockMvc+@SpringBootTest`와 동등한 검증 범위를 슬라이스로 달성.

**커버한 케이스 — `AuthControllerHeaderBindingIntegrationTest` (8건)**

`@TestPropertySource(["mock-oauth.enabled=true"])`로 Mock 빈이 등록된 환경. `X-Mock-OAuth` 헤더 값에 따라 Mock/Real 분기 결과를 KakaoAuthApi 호출 횟수로 구분 검증.

- `X-Mock-OAuth=true 헤더는 useMock=true로 바인딩되어 mock processor 분기로 진입한다`
- `X-Mock-OAuth=false 헤더는 useMock=false로 바인딩되어 real processor 분기로 진입한다`
- `헤더 없음은 defaultValue=false로 바인딩되어 real processor 분기로 진입한다`
- `X-Mock-OAuth=TRUE 대문자는 Spring Boolean 컨버터에 의해 true로 바인딩된다`
- `X-Mock-OAuth=1 숫자는 Spring Boolean 컨버터에 의해 true로 바인딩된다`
- `X-Mock-OAuth=0 숫자는 false로 바인딩된다`
- `X-Mock-OAuth=yes 문자열은 Spring Boolean 컨버터에 의해 true로 바인딩된다`
- `X-Mock-OAuth=garbage 임의 문자열은 400 INVALID_INPUT으로 거절된다`

### Spring 기본 `StringToBooleanConverter` 동작 표 (Major 2 실증 결과)

| `X-Mock-OAuth` 헤더 값 | 바인딩 결과 (`useMock`) | 분기 | 응답 |
|---|---|---|---|
| `true` | `true` | Mock | 200 |
| `TRUE` (대문자) | `true` | Mock | 200 |
| `1` | `true` | Mock | 200 |
| `yes` | `true` | Mock | 200 |
| `false` | `false` | Real | (외부 API 호출) |
| `0` | `false` | Real | (외부 API 호출) |
| 헤더 없음 | `false` (defaultValue) | Real | (외부 API 호출) |
| `garbage` | 바인딩 실패 | — | **400 DE0003 (INVALID_INPUT)** |

**설계서 02_design.md 라인 216-220 주장과의 대조:**

- 설계서 주장: `1 → false, 0 → false, true → true, 빈/기타 → false`.
- **실제 동작:** `1 → true, 0 → false, true → true, TRUE → true, yes → true, garbage → 400 거절`.
- **설계서 주장은 부분적으로 틀렸다.** Spring `StringToBooleanConverter` 기본 동작은 다음 문자열을 `true`로 인식: `"true"`, `"on"`, `"yes"`, `"1"` (대소문자 무시). 다음을 `false`로 인식: `"false"`, `"off"`, `"no"`, `"0"`. 그 외 값은 `IllegalArgumentException` → `MethodArgumentTypeMismatchException` → `WebExceptionHandler.handleInvalidInput` → 400 응답.
- **운영상 함의:** 클라이언트가 `X-Mock-OAuth: yes` 또는 `X-Mock-OAuth: 1`을 의도하지 않게 보내면 mock 분기로 진입한다. prod에서는 `mock-oauth.enabled=false`이므로 mockMap이 비어 결국 400 AC006으로 거절되어 안전하지만, dev/local에서는 클라이언트의 무심한 헤더 값이 mock 분기를 트리거할 수 있음. 설계서 라인 216-220 주장은 본 표로 수정 필요.

**커버한 케이스 — `AuthControllerIntegrationTest` (수정, −1건)**

기존 4건 중 마지막 `service에서 UnsupportedOAuthModeException이 발생하면 400 응답과 AC006 에러코드를 반환한다`는 service-level mocking으로 prod 시나리오를 시뮬레이션한 케이스였음. `AuthControllerProdModeIntegrationTest`가 실제 빈 컨텍스트로 동일 흐름을 통과 검증하므로 제거(대체). 나머지 3건(controller→service 인자 전파 검증)은 유지 — controller layer 슬라이스 의미가 있음.

**실행 결과**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew test
```

- 결과: BUILD SUCCESSFUL
- **전체 통계**: tests=283, skipped=0, failures=0, errors=0
- 본 대응 신규 클래스:
  - `AuthControllerProdModeIntegrationTest` 6/6 통과
  - `AuthControllerHeaderBindingIntegrationTest` 8/8 통과
- `AuthControllerIntegrationTest` 3/3 통과 (1건 제거 후)

**핵심 결정 사항**

- **`@WebMvcTest` + `@Import` 선택**: 이 코드베이스는 임베디드 MongoDB가 없고 `@MockkBean`/`@MockBean` 인프라가 활성화되지 않아 `@SpringBootTest` 풀 컨텍스트가 부팅 실패. `@WebMvcTest`로 슬라이스 + `@Import`로 실제 production 클래스(`AuthService`, `OAuthProcessorFactory`, `KakaoProcessor`, `AppleProcessor`, `MockKakaoProcessor`, `MockAppleProcessor`, `WebExceptionHandler`)를 명시 등록. `@Import` 트리거로 `@ConditionalOnProperty` 평가가 그대로 작동. 검증 범위는 `@SpringBootTest + @AutoConfigureMockMvc`와 동등.
- **Mock 호출 기록 누설 처리**: Spring 컨텍스트 캐싱으로 mock 빈(KakaoAuthApi 등)이 테스트 간 공유됨. `@BeforeEach`의 `clearMocks(..., recordedCalls = true, answers = false)`로 호출 기록만 초기화하고 stubbing은 보존.
- **JSON 역직렬화 — `ObjectMapper` override 회피**: `AppleProcessor` 의존 ObjectMapper를 `@TestConfiguration`에서 `ObjectMapper()`로 빈 override하면 WebMvc 메시지 컨버터의 Kotlin 모듈이 빠져 `LoginRequest` 역직렬화가 깨진다. Spring Boot 기본 auto-configured `ObjectMapper`(Kotlin 모듈 포함)를 그대로 공유.
- **두 별도 클래스로 분리 (컨텍스트 캐시 격리)**: `mock-oauth.enabled=true`/`false`는 `@TestPropertySource`로 분리되어야 각각의 컨디션이 평가됨. 같은 클래스 내 nested로 두면 컨텍스트 캐시가 충돌하므로 별도 파일로 분리.

**미처리 항목 (의도적 제외)**

- Critical 1 (브랜치 위생) + 본 PR 범위 외 변경(colorCode 등): 본 작업 지시 명시적 제외.
- Major 3 (`AuthService.login` default 파라미터 제거): 직전 재실행에서 이미 처리됨 (이 파일 위쪽 "재실행 변경 이력 2026-05-22" 첫 항목 참조).
- Minor 항목들: 본 작업 지시 범위 외.
