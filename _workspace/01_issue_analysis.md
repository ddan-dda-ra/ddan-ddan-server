# 이슈 분석: dev 환경 Mock/Real OAuth 동시 사용 (X-Mock-OAuth 헤더 기반 디스패치)

## 요구사항 요약
dev 환경에서 `mock-oauth.enabled=true`로 인해 Real OAuth processor가 빈에서 제외되어, 실제 카카오/Apple 토큰을 가진 prod 가입 유저로 dev 로그인이 불가능하다. 이를 해소하기 위해 dev 환경에서 Real/Mock processor를 동시에 빈으로 등록하고, 클라이언트가 `X-Mock-OAuth: true` 헤더를 명시할 때만 Mock processor를 사용하도록 디스패치를 변경한다. 헤더가 없거나 false면 Real로 처리(prod와 동일). prod에서는 `mock-oauth.enabled=false`로 Mock 빈이 아예 등록되지 않으므로 헤더가 와도 자연스럽게 Real로 fallback되어야 한다.

## 영향 받는 레이어
- [x] domain — 없음 (도메인 변경 없음)
- [x] application — `OAuthProcessorFactory`, `AuthService` 시그니처 변경 (Mock/Real 디스패치 로직 추가)
- [x] infrastructure — 없음 (외부 API 클라이언트 변경 없음)
- [x] presentation — `AuthController.login`이 `@RequestHeader("X-Mock-OAuth")` 받아 service로 전파, `LoggingFilter`는 헤더 평문 로깅(별도 마스킹 불필요)

## 관련 코드

### Processor 빈 등록 (Conditional 변경 대상)
| 파일 | 라인 | 역할 |
|---|---|---|
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/processor/KakaoProcessor.kt` | 13-14 | `@ConditionalOnProperty(..., havingValue="false", matchIfMissing=true)` — Mock 활성 시 빈에서 제외 (이 조건 제거 필요) |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/processor/AppleProcessor.kt` | 22-23 | KakaoProcessor와 동일 패턴 (조건 제거 필요) |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/processor/MockKakaoProcessor.kt` | 8-9 | `@ConditionalOnProperty(..., havingValue="true")` — 그대로 유지 (mock-oauth.enabled=true인 환경에만 등록) |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/processor/MockAppleProcessor.kt` | 8-9 | 동일 (유지) |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/processor/OAuthProcessor.kt` | 5-9 | 인터페이스. `isMock(): Boolean` 또는 별도 마커 인터페이스(`MockOAuthProcessor`) 도입 후보 |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/processor/OAuthProcessorFactory.kt` | 8-23 | 핵심 변경 지점. `Map<OAuthType, OAuthProcessor>` 단일 맵 → real/mock 두 맵으로 분리. `getClient(type, useMock: Boolean)` 시그니처. mock=true인데 mock 빈이 없으면 real로 fallback |

### Service / Controller 흐름
| 파일 | 라인 | 역할 |
|---|---|---|
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/service/AuthService.kt` | 24, 28-39 | `login(token, type, deviceToken)` 시그니처에 `useMock: Boolean = false` 파라미터 추가. line 33의 `oauthProcessorFactory.getClient(oAuthType)` 호출에 useMock 전달 |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/presentation/controller/AuthController.kt` | 19-30 | `/v1/auth/login` 핸들러에 `@RequestHeader(name="X-Mock-OAuth", required=false, defaultValue="false") useMock: Boolean` 추가. `authService.login(...)` 호출에 전달. `/reissue` 엔드포인트는 헤더 불필요 (JWT refresh만 검증) |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/presentation/dto/request/LoginRequest.kt` | 7-14 | 변경 없음 (헤더로 받기 때문에 body는 그대로) |

### 환경 설정
| 파일 | 라인 | 역할 |
|---|---|---|
| `src/main/resources/application.yaml` | 7-8 | `mock-oauth.enabled: false` (default) — 변경 없음 |
| `src/main/resources/application-dev.yaml` | 35-36 | `mock-oauth.enabled: true` — 변경 없음. 단, 의미가 바뀜 ("Mock processor 빈 등록 여부"로 격하). 주석 추가 권장 |
| `src/main/resources/application-local.yaml` | 35-36 | 동일 (변경 없음) |
| `src/main/resources/application-prod.yaml` | 35-36 | `mock-oauth.enabled: false` — 변경 없음 |

### 필터 / Security
| 파일 | 라인 | 역할 |
|---|---|---|
| `src/main/kotlin/notbe/tmtm/ddanddanserver/presentation/filter/LoggingFilter.kt` | 31-44 | 모든 헤더를 평문 로깅. `X-Mock-OAuth`는 비밀값이 아니므로 마스킹 불필요. 변경 없음 |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/config/WebSecurityConfig.kt` | 106-117 | `loginFilterChain` (`@Order(1)`, `/v1/auth/**` permitAll) — 헤더 검사 없음, 변경 없음 |

### 테스트
| 파일 | 라인 | 역할 |
|---|---|---|
| `src/test/kotlin/notbe/tmtm/ddanddanserver/application/service/AuthServiceTest.kt` | 24-125 | `login(...)` 호출이 3개 테스트에서 모두 명시. useMock 파라미터 추가 시 default=false면 호환되지만, mock=true 경로 분기 테스트 신규 추가 필요 |
| `src/test/kotlin/notbe/tmtm/ddanddanserver/application/processor/MockKakaoProcessorTest.kt` | 7-35 | 단위 테스트 — 변경 없음 |
| `src/test/kotlin/notbe/tmtm/ddanddanserver/application/processor/MockAppleProcessorTest.kt` | 7-28 | 동일 |
| `src/test/kotlin/notbe/tmtm/ddanddanserver/application/processor/KakaoProcessorTest.kt` | 17-148 | 단위 테스트 — 변경 없음 |
| `src/test/kotlin/notbe/tmtm/ddanddanserver/application/processor/AppleProcessorTest.kt` | 14-83 | 동일 |
| (신규) `OAuthProcessorFactoryTest.kt` | — | 신규 작성. mock=true인데 mock 빈 부재 시 real fallback 검증 / mock=true 정상 시 mock 반환 / mock=false면 real 반환 |
| (신규) `AuthControllerIntegrationTest.kt` | — | `X-Mock-OAuth` 헤더 유무에 따라 processor 분기되는지 통합 검증 (없으면 신규 작성, 또는 AuthService 통합 테스트로 갈음) |

## 참조할 기존 패턴

- **`@ConditionalOnProperty` Factory + 단일 Map 패턴 (현재 OAuthProcessorFactory)** — `application/processor/OAuthProcessorFactory.kt:8-23`. 같은 OAuthType 키에 하나의 processor만 매핑. **본 요구사항으로 인해 이 패턴이 깨진다** (한 type당 2개 processor 등록 필요). Factory는 두 개의 맵(realMap, mockMap)으로 분리하는 방식이 가장 깔끔.
- **`@RequestHeader` + default 값 사용 예시** — 현재 컨트롤러에는 `@RequestHeader` 사용처가 없다. Spring 표준 패턴이므로 `@RequestHeader(name = "X-Mock-OAuth", required = false, defaultValue = "false")` 사용. Spring이 String "true"/"false"를 Boolean으로 자동 바인딩.
- **마커 인터페이스 vs 메서드 분기** — `OAuthProcessor` 인터페이스에 `isMock(): Boolean` 디폴트 메서드를 추가하거나, `MockOAuthProcessor : OAuthProcessor` 별도 마커 인터페이스로 구분. 마커 인터페이스가 SOLID 관점에서 더 명시적이고 Factory에서 `is MockOAuthProcessor`로 분류 가능.

## 위험 요소

1. **prod 안전성 — Mock 빈 부재 시 fallback 정책**
   - prod에서 `mock-oauth.enabled=false`이므로 `MockKakaoProcessor`/`MockAppleProcessor` 빈이 등록되지 않음.
   - 악성 또는 실수로 `X-Mock-OAuth: true` 헤더가 와도 mock 맵이 비어 있으므로, **real로 fallback해야 안전**. (4xx로 reject하는 옵션도 가능하지만 클라이언트 실수에 의한 장애 위험이 큼.)
   - 권장: `getClient(type, useMock)`에서 `useMock=true && mockMap[type]==null` → real 사용 + WARN 로그.

2. **`OAuthProcessorFactory` 시그니처 변경의 파급**
   - `getClient(type)` 호출처는 `AuthService.login` 단 1곳 (`AuthService.kt:33`). 변경 영향이 좁다.
   - 기존 시그니처와의 호환을 위해 `getClient(type)` (default useMock=false) overload 유지 가능.

3. **테스트 mock 셋업 변경**
   - `AuthServiceTest.kt:57, 91, 115`의 `every { oauthProcessorFactory.getClient(OAuthType.KAKAO) } returns oauthProcessor` 가 시그니처 변경으로 컴파일 에러 가능 (useMock 파라미터 추가 시).
   - kotlin default param이면 mockk이 `getClient(OAuthType.KAKAO, false)`로 매칭되므로 보통 OK. 단, 인자 매칭 strict 모드일 경우 `any<Boolean>()`으로 보완 필요.

4. **Mock id 충돌 가능성 (dev DB)**
   - dev DB에는 이미 `mock-kakao-XXX`, `mock-apple-XXX` 형태의 oauth_id를 가진 유저 데이터가 있음.
   - Real 토큰의 카카오 id (예: 숫자 `12345`)와 Mock id (`mock-kakao-12345`)는 prefix가 다르므로 충돌 없음.
   - **단, 실제 prod에서 가입한 유저의 oauth_id가 dev DB에 없다면**, dev에서 real 토큰으로 로그인 시 신규 회원가입 흐름(`loginNewUser`) 진입 → dev DB에 새 user 도큐먼트 생성. 이게 의도된 동작인지 확인 필요 (미해결 질문 참조).

5. **`@ConditionalOnProperty` 제거 시 의도치 않은 빈 등록**
   - `KakaoProcessor`/`AppleProcessor`에서 `@ConditionalOnProperty` 제거 시, Mock 빈도 함께 등록되는 dev/local에서 두 processor 모두 빈으로 떠 있음. Factory가 이를 두 맵으로 분리하지 않으면 `associateBy { type }` 시 마지막 한 개만 살아남는 silent 오류 발생.
   - → Factory 변경(real/mock 분리 매핑)이 동시에 반드시 함께 들어가야 한다.

6. **로깅 노이즈**
   - dev에서 헤더 없이 호출하면 매 로그인마다 real OAuth API 호출이 일어남 (현재 mock만 쓰던 때보다 외부 의존도 증가). 카카오 API의 dev 토큰 만료/rate-limit에 노출.

## 미해결 질문

1. **헤더값 형식** — Spring은 `@RequestHeader Boolean` 바인딩 시 `"true"`/`"false"` 문자열을 변환한다. `"1"`/`"0"` 또는 빈 문자열은 false로 처리됨. 명세에 "Boolean"이라 했으므로 `true`/`false` 문자열을 표준으로 한다. 클라이언트(iOS/Android)와 합의되었는지 확인 필요.
2. **prod에서 `X-Mock-OAuth: true` 헤더 수신 시 정책** — fallback(real로 동작) vs reject(400). 안전성 관점에서 fallback 권장. 확정 필요.
3. **dev DB에 신규 가입자 데이터 누적 우려** — dev에서 real 토큰으로 로그인 시 dev DB에 신규 유저가 생성된다. 이게 의도된 동작인가? (참고: 메모리에 "smoke 테스트는 고정 mock 토큰 재사용" 가이드가 있음 — real 토큰을 자주 쓰면 동일한 우려가 dev에도 적용).
4. **Real/Mock 분리 패턴** — (a) `OAuthProcessor` 인터페이스에 `isMock(): Boolean = false` 디폴트 메서드 추가, (b) `MockOAuthProcessor` 마커 인터페이스 신설, (c) Factory가 빈 클래스명(Mock prefix) 기반 분류. 어떤 방식을 선호하는지 확정 필요. 권장: (b) — 가장 명시적이고 SOLID.
5. **`X-Mock-OAuth` 헤더의 Swagger 문서화** — `/v1/auth/login` Operation에 `@Parameter(in = ParameterIn.HEADER, name = "X-Mock-OAuth", required = false)` 어노테이션 추가 필요 여부.
6. **OAuthType.APPLE 어드민 토큰 흐름과의 독립성** — 본 변경은 `/v1/auth/login`에만 영향. 어드민 인증(`AdminJWTAuthFilter`)은 별도 흐름이므로 무영향. 명시 확인.
7. **`reissueToken` 엔드포인트** — refresh 토큰만 검증하므로 mock/real 구분 불필요. 헤더 받지 않음으로 확정.
