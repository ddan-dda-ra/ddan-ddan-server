# 구현 결과

## 변경 파일

| 파일 | 동작 | 요약 |
|---|---|---|
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/processor/MockOAuthProcessor.kt` | 생성 | Mock processor 분류용 마커 인터페이스. `OAuthProcessor`를 상속하며 별도 메서드 없음. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/processor/KakaoProcessor.kt` | 수정 | `@ConditionalOnProperty` 및 관련 import 제거. 항상 빈으로 등록되도록 변경. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/processor/AppleProcessor.kt` | 수정 | `@ConditionalOnProperty` 및 관련 import 제거. 항상 빈으로 등록되도록 변경. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/processor/MockKakaoProcessor.kt` | 수정 | 부모를 `OAuthProcessor` → `MockOAuthProcessor`로 변경. `@ConditionalOnProperty(... havingValue="true")` 유지. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/processor/MockAppleProcessor.kt` | 수정 | 동상. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/processor/OAuthProcessorFactory.kt` | 수정 | `clientMap` 단일 맵 제거 → `realMap`/`mockMap` 두 맵으로 분리. `clients.partition { it is MockOAuthProcessor }`로 분류. `getClient(type, useMock)` 시그니처로 변경. `useMock=true && mock 없음` → `UnsupportedOAuthModeException` throw. `useMock=false && real 없음` → 기존 호환 `IllegalArgumentException` throw. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/service/AuthService.kt` | 수정 | `login(...)` 시그니처에 `useMock: Boolean = false` 파라미터 추가. factory 호출 `getClient(oAuthType, useMock)`로 변경. default `false` 유지로 기존 테스트 호환. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/presentation/controller/AuthController.kt` | 수정 | `@RequestHeader(name = "X-Mock-OAuth", required = false, defaultValue = "false") useMock: Boolean` 파라미터 추가. `RequestHeader` import 추가. `authService.login(...)` 호출에 useMock 전달. Swagger `@Parameter`는 사용자 확정에 따라 미적용. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/domain/exception/ErrorCode.kt` | 수정 | `UNSUPPORTED_OAUTH_MODE("AC006", "요청한 OAuth 모드를 현재 환경에서 사용할 수 없습니다.")` 추가 (인증 카테고리 끝). |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/domain/exception/AuthenticationException.kt` | 수정 | `UnsupportedOAuthModeException(oAuthType: OAuthType) : AuthenticationException(ErrorCode.UNSUPPORTED_OAUTH_MODE, oAuthType)` 클래스 추가. |
| `src/main/kotlin/notbe/tmtm/ddanddanserver/common/WebExceptionHandler.kt` | 수정 | `handleUnsupportedOAuthModeException` 핸들러 추가 → HTTP 400. 부모 `AuthenticationException` 핸들러(401)보다 먼저 선언하여 명시적 우선순위. (Spring은 가장 구체적 타입 매칭이 기본이므로 안전.) `domain.exception.*` wildcard import로 신규 예외 자동 흡수. |

## 핵심 결정

- **WebExceptionHandler 매핑 추가 위치**: 설계서는 "기존 `handleAuthenticationException` 직전 또는 직후"라고 했고, 설계서 본문에 "ExceptionHandler 매처 순서상 `UnsupportedOAuthModeException` 핸들러를 `AuthenticationException` 핸들러보다 먼저 평가하도록 별도 메서드로 추가"라고 명시되어 있으므로 401 핸들러 **직전**에 추가했다. Spring `@ExceptionHandler`는 가장 구체적인 타입을 우선 매칭하므로 순서가 동작에 영향을 주지는 않지만, 가독성을 위해 인접 배치.
- **`UnsupportedOAuthModeException`의 import 추가**: `WebExceptionHandler`가 이미 `domain.exception.*` wildcard import를 사용하므로 별도 import 라인 추가 불필요. 자동으로 흡수됨.
- **`AuthController` 호출처 영향**: AdminAuthController는 `AdminAuthService`를 사용하며 `AuthService`와 무관. 본 변경에 영향 없음.
- **`AuthService.login` default 파라미터 `useMock: Boolean = false`**: 설계서 명세대로 default 유지. 기존 테스트(useMock 미명시 호출)와의 호환 + prod-safe default 보장.

설계 대비 변경 사항: **없음.** 설계서를 그대로 따라 구현했다.

## 빌드 검증

- compileKotlin: ✅
  - 호스트 시스템 기본 `JAVA_HOME`이 JDK 25로 설정되어 있어 Kotlin 1.9.24 컴파일러(`JavaVersion.parse("25")` 미지원)와 호환성 문제가 발생했다. 프로젝트 타깃 JDK 17로 명시 실행하여 (`JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew compileKotlin`) 정상 빌드 확인.
  - 본 PR 변경과 무관한 사전 환경 이슈. 출력에 경고만 존재(기존 `ErrorResponse.kt`의 KT-73255 annotation target 경고)하며 본 PR로 인한 새 경고/오류 없음.

## test-engineer에게 전달할 메모

### 1. `AuthServiceTest` 기존 호출 보강 필요
- 기존 3개 테스트에서 `every { oauthProcessorFactory.getClient(OAuthType.KAKAO) } returns oauthProcessor` 형태로 stubbing. 시그니처 변경(`getClient(type, useMock: Boolean)`)으로 인해 컴파일은 통과해도 mockk 매칭 실패 가능. `getClient(OAuthType.KAKAO, false)` 또는 `getClient(OAuthType.KAKAO, any())`로 수정 필요.
- 신규 케이스: `useMock=true` 전달 시 `verify { oauthProcessorFactory.getClient(OAuthType.KAKAO, true) }` 검증.
- 신규 케이스: `useMock=false` (헤더 미지정/false)일 때 `verify { oauthProcessorFactory.getClient(OAuthType.KAKAO, false) }` 검증.

### 2. `OAuthProcessorFactoryTest` (신규)
- 4가지 케이스 (설계서 테스트 계획 1번 참조):
  - real/mock 함께 주입 시 정확히 분리되는지
  - useMock=false인데 real 없음 → `IllegalArgumentException`
  - useMock=true인데 mock 없음 (prod 시나리오) → `UnsupportedOAuthModeException` throw + `data == OAuthType.KAKAO`
  - useMock=false면 real 우선 (mock 우회 불가)
- Mockk으로 `MockOAuthProcessor` fake 만들 때 `mockk<MockOAuthProcessor>()` 또는 `object : MockOAuthProcessor { ... }`로 작성. `getProviderType()` stubbing만 필요.
- `@PostConstruct init()`은 Spring이 호출. 단위 테스트에서는 인스턴스 생성 후 직접 `factory.init()` 호출해 분류 트리거.

### 3. `AuthControllerIntegrationTest` (필수, `@SpringBootTest` + `MockMvc`)
- CLAUDE.md MEMORY 항목 "Spring 어노테이션 핵심 코드는 통합 테스트 필수" 적용. `@RequestHeader Boolean` 바인딩과 `WebExceptionHandler` 매핑은 통합 테스트로만 검증 가능.
- 시나리오 5종 (설계서 테스트 계획 3번 참조):
  - 헤더 없음 → real 호출
  - `X-Mock-OAuth: false` → real 호출
  - `X-Mock-OAuth: true` → mock 호출 (nickname=`MockKakao_{token}`)
  - `X-Mock-OAuth: true` + `mock-oauth.enabled=false` (별도 `@TestPropertySource`) → **400, errorCode=`AC006`**
  - `X-Mock-OAuth: invalid` → Spring이 false로 바인딩 → real 호출
- `KakaoAuthApi`/`AppleAuthApi`는 `@MockkBean`으로 stubbing하여 외부 API 차단 필수.
- `mock-oauth.enabled=false` 시나리오는 별도 nested 클래스 또는 별도 파일로 구성 (컨텍스트 분리).

### 4. 주의: JDK 환경
- 본 환경에서 default JAVA_HOME이 JDK 25 → Kotlin 1.9.24 컴파일러 비호환. 테스트 실행 시 `JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew test` 형태로 명시.

### 5. 기존 `MockKakaoProcessorTest`/`MockAppleProcessorTest`/`KakaoProcessorTest`/`AppleProcessorTest`
- 마커 인터페이스로 부모만 변경되었고 시그니처 무영향. 단위 테스트는 생성자 직접 호출이라 `@Component`/`@ConditionalOnProperty`와 무관. 컴파일 확인만 필요.

---

## 재실행 변경 이력

### 2026-05-22 — 리뷰 05_review.md Major 3 대응

**범위:** Major 3 (`AuthService.login`의 `useMock: Boolean = false` default 제거)만 처리. Critical 1·2, Major 1·2, Minor 항목 및 본 PR 범위 외 변경(`UserRegisteredEventListener` Discord, PetCatalog 등)은 손대지 않음.

**변경 파일**

| 파일 | 동작 | 요약 |
|---|---|---|
| `src/main/kotlin/notbe/tmtm/ddanddanserver/application/service/AuthService.kt` | 수정 | `login(...)`의 `useMock: Boolean = false` → `useMock: Boolean` (default 제거). "호출자가 명시적으로 결정" 정책과 일관성 회복. 02_design.md 라인 411-415의 `OAuthProcessorFactory.getClient(type, useMock)` 결정과 시그니처 일관. |
| `src/test/kotlin/notbe/tmtm/ddanddanserver/application/service/AuthServiceTest.kt` | 수정 | `authService.login(...)` 4개 호출 모두 `useMock = false` 명시 추가 (66, 97, 120, 152행). 142행 테스트 이름을 "useMock 미지정 시 default false로 factory에 전파된다" → "useMock=false가 전달되면 factory에 useMock=false로 전파된다"로 변경 (default 제거로 의미 변경). |

**핵심 결정**

- `AuthController.login`은 이미 `@RequestHeader(... defaultValue = "false") useMock: Boolean`로 항상 명시 전달이라 컨트롤러 측 영향 없음. `AuthControllerIntegrationTest`도 `authService.login(any(), any(), any(), capture(useMockSlot))`/`authService.login(any(), any(), any(), false|true)`로 4번째 인자를 항상 명시하므로 영향 없음. AdminAuthService 등 다른 `login` 호출자는 별도 서비스이므로 무관.
- `AuthServiceTest`의 4개 default 의존 호출처만 보강. mockk stubbing은 이미 `getClient(OAuthType.KAKAO, false)`로 명시 등록되어 있었으므로 동작 변화 없음.

**빌드 검증**

- `./gradlew compileKotlin`: ✅ (`JAVA_HOME=$(/usr/libexec/java_home -v 17)` 명시 실행, BUILD SUCCESSFUL)
- `./gradlew compileTestKotlin`: ✅ (BUILD SUCCESSFUL — `AuthServiceTest` 포함 모든 테스트 컴파일 통과)

**미처리 항목 (의도적 제외)**

- Critical 1·2, Major 1·2, Minor 모든 항목: 본 작업 지시 범위 외.
- `_workspace_prev_colorcode/`, colorCode 관련 8 파일, Discord/PetCatalog 변경: 본 작업 지시 명시적 제외 ("본 PR 범위 외 변경은 건들지 말 것").
