# 설계: dev 환경 Mock/Real OAuth 동시 사용 (X-Mock-OAuth 헤더 기반 디스패치)

## 결정 사항 요약 (사용자 확정 사항 반영)

| 항목 | 결정 |
|---|---|
| 헤더명 | `X-Mock-OAuth` (Boolean, optional, default `false`) |
| prod 정책 | Mock 빈 미등록 환경에서 `X-Mock-OAuth: true` 수신 시 **400 reject**. Real로 fallback 하지 않음. |
| dev 동일 정책 | dev에서도 `useMock=true`인데 해당 `OAuthType`의 Mock processor가 없으면 동일하게 **400 reject**. |
| Real/Mock 구분 패턴 | `MockOAuthProcessor` 마커 인터페이스 신설 (SOLID 관점에서 가장 명시적). `OAuthProcessorFactory`에서 `clients.partition { it is MockOAuthProcessor }`로 분리. |
| `KakaoProcessor`/`AppleProcessor` | `@ConditionalOnProperty` **제거** → 항상 빈 등록. |
| `MockKakaoProcessor`/`MockAppleProcessor` | `@ConditionalOnProperty(prefix="mock-oauth", name=["enabled"], havingValue="true")` **유지**. |
| `mock-oauth.enabled` 의미 | "Mock processor 빈 등록 여부"로 격하. Real 빈 등록과는 무관. |
| Swagger 문서화 | **안 함** (내부 용도, `@Parameter` 미사용). |
| 신규 도메인 예외 | `UnsupportedOAuthModeException` (extends `AuthenticationException`). `WebExceptionHandler`에서 400으로 매핑. |
| 환경 설정 파일 | `application-*.yaml` 변경 **없음** (`mock-oauth.enabled` 키 그대로 유지). |

## 영향 받는 레이어와 책임

| 레이어 | 변경 |
|---|---|
| domain | `UnsupportedOAuthModeException` 1개 추가, `ErrorCode.UNSUPPORTED_OAUTH_MODE` 1개 추가 |
| application | `MockOAuthProcessor` 마커 인터페이스 신설; `MockKakao/MockAppleProcessor`에 마커 적용; `KakaoProcessor`/`AppleProcessor`의 `@ConditionalOnProperty` 제거; `OAuthProcessorFactory` 내부 맵을 real/mock 2개로 분리, `getClient(type, useMock)` 시그니처 변경; `AuthService.login` 시그니처에 `useMock` 추가 |
| infrastructure | 없음 |
| presentation | `AuthController.login`에 `@RequestHeader("X-Mock-OAuth")` 추가; `WebExceptionHandler`에 신규 예외 매핑(또는 `AuthenticationException` 기존 핸들러 재사용 검토) |

## 도메인 모델

**도메인 모델 신규/수정 없음.** `OAuth`, `OAuthType`, `AuthResult` 모두 무변경. 본 변경은 "어떤 processor가 호출될지"의 디스패치 결정 로직이며, 도메인 데이터 구조 변경이 아니다.

### 도메인 예외 신설 — `UnsupportedOAuthModeException`

`domain/exception/AuthenticationException.kt`에 추가 (기존 파일 내 클래스 추가; 새 파일 신설 안 함). `AuthenticationException`의 하위 클래스로 두는 이유는 "인증 입구에서 거부되는 요청" 카테고리에 정합하기 때문이다. 다만 HTTP 응답은 401이 아니라 **400 Bad Request**이므로 (인증 시도 자체가 잘못된 요청 형태) `WebExceptionHandler`에서 별도 매핑한다.

```kotlin
class UnsupportedOAuthModeException(
    oAuthType: OAuthType,
) : AuthenticationException(ErrorCode.UNSUPPORTED_OAUTH_MODE, oAuthType)
```

데이터로 `oAuthType`을 실어서 클라이언트 디버깅에 도움.

### `ErrorCode` 추가

`domain/exception/ErrorCode.kt` 인증 카테고리 또는 기본 카테고리에 추가:

```kotlin
UNSUPPORTED_OAUTH_MODE("AC006", "요청한 OAuth 모드를 현재 환경에서 사용할 수 없습니다."),
```

코드 prefix `AC`는 인증(authentication) 카테고리 컨벤션과 일치.

## API 명세

### `POST /v1/auth/login` — 요청 헤더 추가

| Method | Path | Request Body | Request Header | Response | Auth |
|---|---|---|---|---|---|
| POST | `/v1/auth/login` | `LoginRequest` (무변경) | `X-Mock-OAuth: Boolean` (optional, default `false`) | `LoginResponse` (무변경) | 공개 (`/v1/auth/**` permitAll) |

요청 예시:

```http
POST /v1/auth/login HTTP/1.1
Content-Type: application/json
X-Mock-OAuth: true

{
  "token": "12345",
  "tokenType": "KAKAO",
  "deviceToken": "..."
}
```

응답 시나리오:

| 환경 | 헤더 | 결과 |
|---|---|---|
| prod | 없음 또는 `false` | Real processor 호출 (기존과 동일) |
| prod | `true` | **400 `UNSUPPORTED_OAUTH_MODE`** (mock 빈 미등록) |
| dev | 없음 또는 `false` | Real processor 호출 (실제 카카오/Apple API 호출) |
| dev | `true` | Mock processor 호출 (`mock-kakao-{token}` 등) |
| dev (mock-oauth.enabled=false인 가상 케이스) | `true` | **400 `UNSUPPORTED_OAUTH_MODE`** |

### `POST /v1/auth/reissue` — 무변경

refresh 토큰만 검증하므로 `X-Mock-OAuth` 헤더 영향 없음.

### Swagger / OpenAPI

`@Parameter` 추가하지 **않는다** (사용자 확정). 헤더는 내부용도이며 외부 문서 노출 불필요.

## 서비스 시그니처

### `AuthService.login(...)` — 시그니처 변경

```kotlin
@Service
class AuthService(...) {
    @Transactional
    fun login(
        oAuthAccessToken: String,
        oAuthType: OAuthType,
        deviceToken: String?,
        useMock: Boolean = false,        // 신규 (default false: prod 안전)
    ): AuthResult {
        val oAuth = oauthProcessorFactory
            .getClient(oAuthType, useMock)   // 신규 시그니처
            .getOAuth(oAuthAccessToken)
        // 이하 기존 로직 동일
    }

    // reissueToken: 무변경
}
```

**default 값을 `false`로 둔 이유:**
- 기존 호출처 호환 (테스트 등).
- prod-safe default (악의적 false → mock 우회 불가).

### `OAuthProcessorFactory.getClient(...)` — 시그니처 변경 + 내부 로직 재설계

```kotlin
@Component
class OAuthProcessorFactory(
    private val clients: List<OAuthProcessor>,
) {
    private lateinit var realMap: Map<OAuthType, OAuthProcessor>
    private lateinit var mockMap: Map<OAuthType, OAuthProcessor>

    @PostConstruct
    fun init() {
        val (mocks, reals) = clients.partition { it is MockOAuthProcessor }
        realMap = reals.associateBy { it.getProviderType() }
        mockMap = mocks.associateBy { it.getProviderType() }
    }

    fun getClient(type: OAuthType, useMock: Boolean): OAuthProcessor {
        val pool = if (useMock) mockMap else realMap
        return pool[type] ?: run {
            if (useMock) {
                throw UnsupportedOAuthModeException(type)
            }
            throw IllegalArgumentException("Unknown OAuth type: $type")
        }
    }
}
```

**왜 `partition` 한 줄로 분리하는가:**
- `MockOAuthProcessor` 마커 인터페이스로 명시적 분류 가능.
- `associateBy { it.getProviderType() }`만으로는 같은 type이 2개(Real + Mock)일 때 마지막 1개만 살아남는 silent bug가 발생. partition으로 사전 분리.
- 신규 OAuth provider 추가 시 별도 if/when 분기 없이 자동 분류 (open-closed).

**왜 `useMock=true && mockMap[type]==null`을 도메인 예외로 던지는가:**
- prod에서 헤더 위조/실수 방지 (real fallback 시 mock 토큰으로 real API 호출 → 카카오/Apple API에서 4xx 발생하지만, 그 4xx가 우리 의도와 무관한 외부 에러로 가려진다).
- 명시적 400으로 클라이언트 측 버그를 빠르게 발견 가능.
- 헤더 자체가 무력화되는 이중 안전 장치(아래 "보안 검토" 참조).

**왜 `useMock=false && realMap[type]==null`은 `IllegalArgumentException`인가:**
- 이 경로는 OAuthType enum이 모든 provider를 커버한다는 가정 하에 발생해선 안 됨. 발생 시 코드 버그(새 OAuthType 추가 후 Processor 미구현).
- 기존 동작과 동일하게 `IllegalArgumentException` → `WebExceptionHandler`에서 `INVALID_INPUT` 400으로 매핑됨. 호환성 유지.

### `MockOAuthProcessor` 마커 인터페이스 (신규)

`application/processor/MockOAuthProcessor.kt`:

```kotlin
package notbe.tmtm.ddanddanserver.application.processor

/**
 * Mock OAuth processor 분류용 마커 인터페이스.
 *
 * - dev/local profile 등 `mock-oauth.enabled=true` 환경에서만 빈으로 등록되는 processor가 구현.
 * - `OAuthProcessorFactory`는 이 마커를 기준으로 real/mock 풀을 분리한다.
 * - 별도 메서드 없이 분류 목적에만 사용 (메서드 추가 시 `OAuthProcessor` 인터페이스를 오염시키지 않기 위한 SOLID 분리).
 */
interface MockOAuthProcessor : OAuthProcessor
```

적용 클래스:
- `MockKakaoProcessor : MockOAuthProcessor` (기존 `OAuthProcessor` 직접 구현 → 마커 인터페이스 구현으로 변경; 마커가 `OAuthProcessor`를 상속하므로 동작 동일)
- `MockAppleProcessor : MockOAuthProcessor` (동상)

`KakaoProcessor`/`AppleProcessor`는 그대로 `OAuthProcessor` 직접 구현 (마커 미적용).

## 컨트롤러 시그니처

### `AuthController.login(...)` — 헤더 파라미터 추가

```kotlin
@PostMapping("/login")
fun login(
    @RequestBody request: LoginRequest,
    @RequestHeader(name = "X-Mock-OAuth", required = false, defaultValue = "false")
    useMock: Boolean,
): LoginResponse {
    val result = authService.login(
        request.token,
        request.tokenType,
        request.deviceToken,
        useMock,
    )

    return LoginResponse.fromDomain(
        accessToken = result.accessToken,
        refreshToken = result.refreshToken,
        user = result.user,
    )
}
```

`refresh(...)`는 무변경.

**Spring 헤더 → Boolean 바인딩 규칙:**
- `"true"` (대소문자 무관) → `true`
- `"false"`, `"1"`, `"0"`, 빈 문자열, 기타 → `false`
- 헤더 자체가 없음 → `defaultValue = "false"`로 fallback → `false`

이 동작은 안전한 default (실수로 다른 값을 보내도 `false`로 해석되어 real로 흘러감). 단 `1` → `false`인 점은 클라이언트 합의 시 명시 (이슈 분석 미해결 질문 1번 → "true/false 문자열로 합의").

## MongoDB 스키마

**변경 없음.** 본 변경은 인증 디스패치 로직만 다루며 DB 도큐먼트/인덱스/마이그레이션 모두 무관.

## 예외

### 신규 도메인 예외

| 클래스 | 위치 | 부모 | ErrorCode | HTTP 상태 |
|---|---|---|---|---|
| `UnsupportedOAuthModeException` | `domain/exception/AuthenticationException.kt`에 클래스 추가 | `AuthenticationException` | `UNSUPPORTED_OAUTH_MODE` (신규 `AC006`) | **400 Bad Request** |

### `WebExceptionHandler` 매핑

기존 `handleAuthenticationException`은 모든 `AuthenticationException`을 **401 UNAUTHORIZED**로 매핑한다. 그러나 `UnsupportedOAuthModeException`은 "환경 정책 위반"이므로 **400**이 의미상 맞다 (인증 시도 자체가 부적절한 요청).

따라서 `UnsupportedOAuthModeException`을 별도 핸들러로 잡아 400으로 응답한다. ExceptionHandler 매처 순서상, **`UnsupportedOAuthModeException` 핸들러를 `AuthenticationException` 핸들러보다 먼저** 평가하도록 별도 메서드로 추가 (Spring `@ExceptionHandler`는 가장 구체적인 타입 우선 매칭이 기본이므로 자동 처리되지만, 명시적으로 둔다).

`common/WebExceptionHandler.kt`에 추가:

```kotlin
@ExceptionHandler(value = [UnsupportedOAuthModeException::class])
fun handleUnsupportedOAuthModeException(
    exception: UnsupportedOAuthModeException,
    request: HttpServletRequest,
): ResponseEntity<ErrorResponse> =
    ResponseEntity
        .badRequest()
        .body(
            ErrorResponse.fromErrorCode(
                errorCode = exception.errorCode,
                data = exception.data,
            ),
        )
```

**대안 비교:**
- (A) `AuthenticationException` 상속 + 별도 400 핸들러 (채택): 카테고리상 가장 적합하고, 기존 401 핸들러는 더 구체적인 핸들러가 먼저 매칭되어 그대로 안전.
- (B) `CustomException` 직접 상속 → 기본 400 핸들러가 자동 처리: 가능하지만 카테고리(인증 입구) 분류가 흐려짐.
- (C) `IllegalArgumentException`으로 던지기: 의미가 모호하고 디버깅 시 트레이스가 부족. **탈락.**

## 환경 설정

`application.yaml`, `application-dev.yaml`, `application-local.yaml`, `application-prod.yaml` 모두 **변경 없음**.

`mock-oauth.enabled` 키의 의미가 바뀐다 ("Mock processor 빈 등록 여부"로 격하). dev/local yaml에 주석으로 명시하는 것은 implementer가 결정 (선택 사항).

## 필터 / Security

**변경 없음.**

- `LoggingFilter` — 모든 헤더를 평문 로깅. `X-Mock-OAuth`는 비밀값 아님. 마스킹 불필요.
- `WebSecurityConfig.loginFilterChain` (`@Order(2)`, `/v1/auth/**` permitAll) — 헤더 검사 없음.

## 변경 파일 목록

### 생성

| 파일 | 내용 |
|---|---|
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/processor/MockOAuthProcessor.kt` | `MockOAuthProcessor` 마커 인터페이스 정의 (3-5줄 + KDoc) |

### 수정 (라인 단위)

| 파일 | 위치/라인 | 변경 내용 |
|---|---|---|
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/processor/KakaoProcessor.kt` | 라인 9 (import) | `import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty` **제거** |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/processor/KakaoProcessor.kt` | 라인 14 | `@ConditionalOnProperty(...)` **제거** (`@Component`만 유지) |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/processor/AppleProcessor.kt` | 라인 14 (import) | `import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty` **제거** |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/processor/AppleProcessor.kt` | 라인 23 | `@ConditionalOnProperty(...)` **제거** (`@Component`만 유지) |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/processor/MockKakaoProcessor.kt` | 라인 10 | `class MockKakaoProcessor : OAuthProcessor` → `class MockKakaoProcessor : MockOAuthProcessor` (`@ConditionalOnProperty`는 유지) |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/processor/MockAppleProcessor.kt` | 라인 10 | 동일 — 부모를 `MockOAuthProcessor`로 변경 |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/processor/OAuthProcessorFactory.kt` | 전체 (라인 7-23) | `clientMap` → `realMap`/`mockMap`으로 분리; `init()`에 `partition` 로직; `getClient(type, useMock)` 시그니처; `useMock=true && mock 없음` 시 `UnsupportedOAuthModeException` throw |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/processor/OAuthProcessorFactory.kt` | import 추가 | `notbe.tmtm.ddanddanserver.domain.exception.UnsupportedOAuthModeException` |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/service/AuthService.kt` | 라인 28-32 | `login(...)` 시그니처에 `useMock: Boolean = false` 파라미터 추가 |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/service/AuthService.kt` | 라인 33 | `oauthProcessorFactory.getClient(oAuthType)` → `oauthProcessorFactory.getClient(oAuthType, useMock)` |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/presentation/controller/AuthController.kt` | 라인 8 (import) | `org.springframework.web.bind.annotation.RequestHeader` 추가 |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/presentation/controller/AuthController.kt` | 라인 19-23 | `login` 시그니처에 `@RequestHeader(name = "X-Mock-OAuth", required = false, defaultValue = "false") useMock: Boolean` 추가; `authService.login(...)` 호출에 `useMock` 전달 |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/domain/exception/ErrorCode.kt` | 라인 25 직후 (인증 카테고리 끝) | `UNSUPPORTED_OAUTH_MODE("AC006", "요청한 OAuth 모드를 현재 환경에서 사용할 수 없습니다.")` 추가 |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/domain/exception/AuthenticationException.kt` | 파일 끝 | `UnsupportedOAuthModeException(oAuthType: OAuthType) : AuthenticationException(ErrorCode.UNSUPPORTED_OAUTH_MODE, oAuthType)` 클래스 추가 |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/common/WebExceptionHandler.kt` | 라인 102 직전 또는 직후 | `@ExceptionHandler(UnsupportedOAuthModeException::class)` 핸들러 추가 (400 응답) |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/common/WebExceptionHandler.kt` | import | `notbe.tmtm.ddanddanserver.domain.exception.UnsupportedOAuthModeException` 추가 (또는 기존 `domain.exception.*` wildcard로 자동 흡수) |

### 무변경 (영향 없음)

- `src/main/kotlin/.../application/processor/OAuthProcessor.kt` (마커 인터페이스 도입으로 `isMock()` 같은 메서드 불필요)
- `src/main/kotlin/.../presentation/dto/request/LoginRequest.kt` (body 변경 없음)
- `src/main/kotlin/.../presentation/filter/LoggingFilter.kt`
- `src/main/kotlin/.../config/WebSecurityConfig.kt`
- `src/main/resources/application.yaml` 및 profile별 yaml
- `src/main/kotlin/.../infrastructure/api/KakaoAuthApi.kt`, `AppleAuthApi.kt`

### 테스트 파일

test-engineer가 별도로 추가/보강 (아래 "테스트 계획" 참고).

## 테스트 계획

### 1. `OAuthProcessorFactoryTest` (신규)

`src/test/kotlin/notbe/tmtm/ddanddanserver/application/processor/OAuthProcessorFactoryTest.kt`:

| 테스트 케이스 | 검증 |
|---|---|
| `partition: real과 mock processor가 함께 주입되면 두 맵으로 정확히 분리된다` | 직접 4개 빈(Real Kakao/Apple + Mock Kakao/Apple) 주입 → `getClient(KAKAO, false)`가 RealKakao, `getClient(KAKAO, true)`가 MockKakao 반환 |
| `useMock=false인데 real 없음` | Real 1개 누락 시 `IllegalArgumentException` |
| `useMock=true인데 mock 없음 (prod 시나리오)` | Mock 빈 없는 상태에서 `getClient(KAKAO, true)` → `UnsupportedOAuthModeException` throw; `data == OAuthType.KAKAO` |
| `useMock=false면 real이 항상 우선` | mock 빈이 함께 있어도 `getClient(KAKAO, false)`는 Real 반환 (mock 우회 불가 확인) |

Mockk으로 `MockOAuthProcessor` 구현체 fake를 만들어 `getProviderType()` 반환만 stubbing.

### 2. `AuthServiceTest` (기존 보강)

`src/test/kotlin/notbe/tmtm/ddanddanserver/application/service/AuthServiceTest.kt`:

- 기존 3개 `login(...)` 호출에 `useMock = false` 명시 (default param이 있어도 mockk verify 호환을 위해 명시) 또는 `verify { ... getClient(any(), any()) }` 패턴.
- 신규 테스트: `useMock=true이면 factory에 useMock=true가 전달된다` — `verify { oauthProcessorFactory.getClient(OAuthType.KAKAO, true) }`.
- 신규 테스트: `useMock=false이면 factory에 useMock=false가 전달된다` — `verify { oauthProcessorFactory.getClient(OAuthType.KAKAO, false) }`.

### 3. `AuthControllerIntegrationTest` (신규 또는 기존 보강) — **필수**

CLAUDE.md MEMORY 항목 "Spring 어노테이션 핵심 코드는 통합 테스트 필수" — `@RequestHeader` 바인딩과 `WebExceptionHandler` 매핑은 `@SpringBootTest` + `MockMvc`로 검증해야 한다.

`src/test/kotlin/notbe/tmtm/ddanddanserver/presentation/controller/AuthControllerIntegrationTest.kt`:

| 시나리오 | 환경 | 헤더 | 기대 결과 |
|---|---|---|---|
| 헤더 없음 → real 호출 | `mock-oauth.enabled=true` (test profile) | (없음) | RealKakaoProcessor mock bean이 호출됨 (200) |
| `X-Mock-OAuth: false` → real 호출 | 동상 | `false` | RealKakaoProcessor 호출 |
| `X-Mock-OAuth: true` → mock 호출 | 동상 | `true` | MockKakaoProcessor 호출 (200, user nickname == `MockKakao_{token}`) |
| `X-Mock-OAuth: true` (prod 시나리오) → 400 | `mock-oauth.enabled=false` (별도 `@TestPropertySource`) | `true` | **400 응답, errorCode == `AC006` (`UNSUPPORTED_OAUTH_MODE`)** |
| `X-Mock-OAuth: invalid_value` | `mock-oauth.enabled=true` | `not-a-bool` | Spring이 `false`로 바인딩 → real 호출 (200). 정책 명시. |

**핵심 포인트:**
- `mock-oauth.enabled=false` 시나리오는 `@TestPropertySource(properties = ["mock-oauth.enabled=false"])`를 적용한 별도 nested 테스트 클래스 또는 별도 통합 테스트 파일로 구성. 컨텍스트 분리 필요.
- `KakaoAuthApi`/`AppleAuthApi`는 `@MockkBean` 또는 `@MockBean`으로 stubbing하여 외부 API 호출 차단.

### 4. `MockKakaoProcessorTest`, `MockAppleProcessorTest` — 무변경

마커 인터페이스로 부모만 변경 (시그니처 무영향). 컴파일만 확인.

### 5. `KakaoProcessorTest`, `AppleProcessorTest` — 무변경

`@ConditionalOnProperty` 제거는 빈 등록 조건만 영향. 단위 테스트는 `@Component`와 무관하게 생성자 직접 호출이므로 영향 없음.

## 보안 검토

### 헤더가 prod에서 무력화되는 이중 안전 장치

| 방어선 | 메커니즘 |
|---|---|
| 1차 (빈 등록) | prod yaml에 `mock-oauth.enabled=false` → `MockKakaoProcessor`/`MockAppleProcessor` 빈이 아예 생성되지 않음 → `mockMap`이 빈 Map |
| 2차 (런타임 검사) | `useMock=true`인데 `mockMap[type] == null` → `UnsupportedOAuthModeException` (400) throw. Real로 fallback하지 않음 |

**공격 시나리오 분석:**
- 공격자가 prod로 `X-Mock-OAuth: true` 전송 → 2차 방어선에서 400. mock 시드 토큰(`mock-kakao-xxx`)으로 mock OAuth id 생성 불가. real 토큰을 갖고 있지 않으면 어떤 유저로도 로그인 불가.
- 실수로 클라이언트가 prod에 헤더를 보냄 → 400 응답을 받아 즉시 인지. 조용히 real fallback되어 부정확한 동작이 누적되는 사고 방지.

### dev 환경 위험

- dev에서 real 토큰으로 로그인 시 dev DB에 신규 user 도큐먼트 생성될 수 있음 (이슈 분석 미해결 질문 3번). 본 변경 범위 외. 메모리 항목 "smoke 테스트는 고정 mock 토큰 재사용"으로 운영적 회피 권장.
- dev에서 real OAuth API 호출 빈도 증가 → 카카오 dev 앱 quota/rate-limit 영향. 모니터링 권장.

### 로깅 / 감사

- `LoggingFilter`가 모든 헤더를 평문 로깅 → `X-Mock-OAuth: true` 사용 흔적이 로그에 남음. 별도 알람은 불필요하나, prod에서 `UNSUPPORTED_OAUTH_MODE` 400 발생 시 알람 추가 검토 가능 (운영 영역).

## 결정 이유

### 왜 마커 인터페이스 (MockOAuthProcessor) 인가

- **명시적**: 클래스 선언만 봐도 Mock인지 즉시 식별. `isMock(): Boolean = false` 디폴트 메서드는 `OAuthProcessor` 인터페이스를 오염시키고, 누구든 `override fun isMock() = true`로 우회 가능.
- **OCP (Open-Closed)**: 새 Mock provider 추가 시 `MockOAuthProcessor`만 구현하면 Factory가 자동 분류. Factory 로직 수정 불필요.
- **SOLID 분리**: 마커는 분류 책임만 가지며, 동작 추가 시 별도 메서드가 마커에 추가될 수 있음. Real processor는 마커를 모름.
- **대안 (a) `isMock(): Boolean`**: 인터페이스 오염 + 모든 구현체가 명시 부담. **탈락.**
- **대안 (c) 클래스명 prefix `Mock` 분류**: 리네임 시 깨짐, 컴파일 안전 X. **탈락.**

### 왜 prod에서 fallback이 아니라 400 reject인가 (사용자 확정)

- **명시적 오류 우선**: 클라이언트 버그로 의도치 않게 헤더가 박혀서 보내질 경우, real fallback은 정상으로 보이지만 실제로는 잘못된 호출 패턴이 누적된다. 400을 받으면 즉시 인지.
- **공격 표면 축소**: real fallback 시 mock 시드 토큰으로 real API 호출이 일어남 (의미 없는 외부 API 호출 발생). reject가 더 깔끔.
- **일관성**: dev/prod 동일 정책 → 환경별 분기 코드 불필요.

### 왜 `UnsupportedOAuthModeException`이 `AuthenticationException` 자식인가 (그러나 HTTP 400)

- 카테고리상 "인증 시도 진입점에서의 거부"가 가장 자연스러운 분류. `domain/exception/AuthenticationException.kt` 같은 파일에 두면 응집도가 높음.
- HTTP 상태 코드는 핸들러에서 결정. 부모 401(`AuthenticationException` 일반)과 다른 400을 의도적으로 사용 — "인증 자체의 실패"가 아닌 "요청 형태/환경 정책 위반"이기 때문.
- ErrorCode `AC006`은 인증(AC) 코드 네임스페이스 일관성 유지.

### 왜 `getClient` 시그니처에 default 파라미터 안 두는가

- `getClient(type)` 단일 시그니처 유지가 호출 의도를 명확히 한다 ("어떤 풀에서 가져올지 호출자가 반드시 결정").
- `AuthService.login`은 `useMock` 파라미터를 받으므로 Factory에 default가 있어도 의미 없음.
- 단, `AuthService.login`의 `useMock`은 default `false`를 유지 — 기존 테스트(`useMock` 미명시) 호환과 prod-safe default 보장.

### 왜 `application-*.yaml`을 안 바꾸는가

- `mock-oauth.enabled` 키는 그대로 사용. 의미만 격하 (Mock 빈 등록 여부).
- yaml 변경 없이 동작 변경이 가능 → 운영 변수 누락 위험 없음.
- 의미 명세는 코드/문서로 보강 (구현 시 `MockKakaoProcessor` 등의 `@ConditionalOnProperty` 옆 주석으로 명시 권장).

### 왜 `LoginRequest` body가 아니라 헤더로 받는가

- "이 토큰을 어떻게 처리할지"는 **요청의 메타 정보** (인프라/디스패치 정보) — body의 도메인 데이터와 별개.
- iOS/Android 클라이언트가 로그인 모드 토글 시 별도 build flavor / 환경 변수 → 헤더 자동 주입이 더 자연스러움.
- 운영적으로 헤더는 LB/프록시 단에서 필터링/모니터링 가능. body 내부 필드보다 관찰성 우수.
- 본 헤더는 비밀값 아님 → `LoggingFilter` 평문 로깅 OK.
