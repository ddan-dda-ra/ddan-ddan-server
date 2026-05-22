# 리뷰 결과

## 요약

- 🛑 **Critical 차단** — PR 위생(브랜치 분리 미흡 + 무관한 변경 혼입) 1건 + 통합 테스트 시나리오 누락 1건. 코드 자체 품질은 양호하나, 현재 상태로 PR을 만들면 다시 #311 사고 패턴이 재발한다.

---

## Critical (반드시 수정)

- [ ] **현재 브랜치가 `feature/#310-pet-catalog-color-code` — 직전 PR #311 사고 패턴 재발 직전.**
  - `git branch --show-current` → `feature/#310-pet-catalog-color-code`
  - `git log develop..HEAD --oneline` → 이전 colorCode 커밋 2건이 남아 있음 (`001e4a1`, `d0493f4`).
  - `git diff develop --stat`에 mock OAuth 변경(11파일)과 함께 **petcatalog/colorCode 관련 무관 파일 8개**가 같이 잡힌다: `application/event/UserRegisteredEventListener.kt`, `application/service/PetCatalogService.kt`, `domain/model/petcatalog/PetCatalog.kt`, `infrastructure/database/entity/PetCatalogEntity.kt`, `infrastructure/database/seed/PetCatalogSeeder.kt`, `presentation/controller/admin/PetCatalogAdminController.kt`, `presentation/dto/admin/PetCatalogAdminDto.kt`, `presentation/dto/response/PetCatalogResponse.kt`. 그리고 해당 테스트 4건도.
  - 권고:
    1. develop에서 새 브랜치 `feature/#XXX-mock-oauth-header-dispatch` 분기.
    2. 이번 PR 대상 파일만 cherry-pick 또는 `git restore`로 develop과의 차이를 mock OAuth 11개 + 테스트 4개로 한정.
    3. colorCode 관련 변경은 별도 작업이거나 이미 머지된 #310 작업의 잔여라면 develop에서 정리.
    4. `_workspace_prev_colorcode/`는 untracked. 작업 분리 후 삭제하거나 `.gitignore`에 추가.

- [ ] **`mock-oauth.enabled=false` + `X-Mock-OAuth: true` 조합의 통합 테스트가 누락되어 본 PR의 핵심 안전 장치가 검증되지 않았다.** — `src/test/kotlin/notbe/tmtm/ddanddanserver/presentation/controller/AuthControllerIntegrationTest.kt`
  - 04_test_summary.md의 4번째 케이스는 “service에서 UnsupportedOAuthModeException이 throw되면 400을 받는다”인데, 이는 단지 `every { authService.login(...) } throws UnsupportedOAuthModeException(...)`로 **서비스 mocking**이라 prod 시나리오(`mockMap`이 비어 있어 `OAuthProcessorFactory`가 직접 예외를 던지는 흐름)를 통과 검증한 게 아니다.
  - 설계서 `_workspace/02_design.md` 라인 348-356은 명시적으로 “`mock-oauth.enabled=false` 시나리오는 `@TestPropertySource(properties = ["mock-oauth.enabled=false"])`로 별도 nested 클래스 또는 별도 파일”을 요구했다. 또한 “`@SpringBootTest` + `MockMvc`로 검증”이라고 명시했는데 현재 구현은 `@WebMvcTest`로 우회.
  - CLAUDE.md MEMORY 항목 “Spring 어노테이션 핵심 코드는 통합 테스트 필수”의 의도(PR #277 사고 재발 방지)에 비추어, `@RequestHeader Boolean` 바인딩 + `@ConditionalOnProperty` 조합 + `OAuthProcessorFactory.partition` 동작은 실제 컨텍스트 부팅으로 한 번은 검증되어야 한다.
  - 권고: `AuthControllerIntegrationTest`에 케이스 추가 또는 별도 `AuthLoginMockDisabledIntegrationTest`를 `@SpringBootTest(properties = ["mock-oauth.enabled=false", "spring.profiles.active="])` + `@AutoConfigureMockMvc`로 작성. `KakaoAuthApi`/`AppleAuthApi`/`MongoTemplate`/`AuthRepository`/`UserRepository` 등은 `@MockkBean`/`@MockBean`으로 stub. 테스트 시나리오:
    - `mock-oauth.enabled=false` 컨텍스트에서 `X-Mock-OAuth: true` 전송 → 400, `code=AC006`, `data=KAKAO` (이때 진짜로 mock 빈이 없어 `OAuthProcessorFactory`가 `UnsupportedOAuthModeException`을 throw하는 흐름).
    - 같은 컨텍스트에서 `X-Mock-OAuth: true`를 APPLE로 전송 → `data=APPLE`.

---

## Major (수정 권장)

- [ ] **`UnsupportedOAuthModeException` 핸들러가 “부모보다 우선 매칭”된다는 보장을 실제 매처로 검증하지 않았다.** — `src/test/kotlin/notbe/tmtm/ddanddanserver/common/WebExceptionHandlerTest.kt:160-179`
  - 추가된 두 번째 케이스 “부모 AuthenticationException 401 핸들러보다 우선 매칭된다”는 `handleUnsupportedOAuthModeException(...)`을 **직접 호출**할 뿐, Spring의 `ExceptionHandlerMethodResolver`가 자식 타입을 선택하는지를 검증하지 못한다. 다른 누군가 무심코 `@ExceptionHandler(AuthenticationException::class)` 핸들러로 통합하면 silent하게 401로 회귀해도 이 테스트는 통과한다.
  - 권고:
    1. 위 Critical의 통합 테스트가 prod 시나리오에서 실제로 400을 받는지 검증하면 이 우선순위가 함께 보장된다.
    2. 또는 `ExceptionHandlerMethodResolver(WebExceptionHandler::class.java).resolveMethod(UnsupportedOAuthModeException(...))` 가 `handleUnsupportedOAuthModeException`인지 단위 검증.

- [ ] **`@RequestHeader(... defaultValue = "false") useMock: Boolean`에 invalid 값(`X-Mock-OAuth: yes`/`X-Mock-OAuth: 1`)이 들어왔을 때의 실제 동작이 검증되지 않았다.** — `src/main/kotlin/notbe/tmtm/ddanddanserver/presentation/controller/AuthController.kt:23`
  - 설계서 02_design.md 라인 216-220 (“`1` → `false`, `0` → `false`, `true` → `true`, 빈/기타 → `false`”)는 사실과 다를 수 있다. Spring `WebDataBinder`의 기본 Boolean 컨버터는 `"true"`/`"on"`/`"yes"`/`"1"` → true, `"false"`/`"off"`/`"no"`/`"0"` → false, 그 외는 `MethodArgumentTypeMismatchException` → `INVALID_INPUT` 400을 던질 수 있다.
  - 04_test_summary.md에는 “invalid 값” 케이스가 빠져 있다(설계서 라인 353에는 있었음). 클라이언트가 `X-Mock-OAuth: yes`를 보내면 의도와 달리 `useMock=true`로 인식될 수 있다.
  - 권고:
    1. 통합 테스트에 invalid 값(`yes`, `1`, `0`, `garbage`) 케이스를 추가해 실제 바인딩 동작을 lock-down.
    2. 또는 `@RequestHeader(...) useMock: String?` 로 받고 컨트롤러 내부에서 `"true".equals(it, ignoreCase = true)` 명시 비교로 단순화. 설계서가 정의한 의미를 코드로 직접 보장 가능. (간단한 수정.)
  - Minor가 아닌 Major로 분류한 이유: 설계서의 문서상 행동 정의와 실제 Spring 동작이 어긋날 가능성이 검증 없이 코드에 반영되어 있다.

- [ ] **`AuthService.login`의 `useMock: Boolean = false` default 파라미터가 호출처에서의 누락을 silent하게 허용한다.** — `src/main/kotlin/notbe/tmtm/ddanddanserver/application/service/AuthService.kt:32`
  - 컨트롤러가 `useMock`을 항상 전달하므로 default가 무의미하다 (03_implementation.md 라인 24의 정당화 “기존 테스트 호환”은 단위 테스트에서만 의미). 향후 누군가 `useMock` 인자를 빼먹고 호출해도 컴파일이 통과 → 본인도 모르게 prod-real로 흘러간다(이번엔 default false라 안전 방향이지만, default 자체가 “호출자가 결정”이라는 정책에 모순).
  - 권고:
    - 옵션 A (간소 — 본 PR 범위 내): default 값을 제거하고, `AuthServiceTest` 기존 호출을 `useMock = false` 명시로 일괄 갱신. 02_design.md 라인 411-415의 `OAuthProcessorFactory.getClient(type, useMock)`에 default 없는 결정과 일관성 회복.
    - 옵션 B (보수적): default 유지하되 `@Deprecated`로 호출 누락을 경고. 본 PR 범위 외라면 후속 이슈로 기록만.

---

## Minor (선택)

- [ ] **`KakaoProcessor`/`AppleProcessor`에서 `@ConditionalOnProperty` 제거 후, “Real 빈이 무조건 등록된다”는 사실에 대한 인라인 주석이 없다.** — `KakaoProcessor.kt:12`, `AppleProcessor.kt:21`
  - 미래의 누군가가 `mock-oauth.enabled=true`인 dev 환경에서 “왜 Real이 같이 있지?”라며 다시 `@ConditionalOnProperty`를 붙일 위험. 한 줄 KDoc으로 “Real은 항상 등록. Mock과 공존하며 OAuthProcessorFactory가 X-Mock-OAuth 헤더로 분기한다.” 추가 권장.

- [ ] **`MockOAuthProcessor` 마커가 `OAuthProcessor`만 상속할 뿐 검증 메커니즘이 없어, 신규 Mock processor 추가 시 마커 누락이 silent하다.** — `application/processor/MockOAuthProcessor.kt`
  - 컴파일 시 강제할 방법은 없지만, `OAuthProcessorFactory.init()`에서 “mock으로 분류된 processor가 mock prefix 이름인지” sanity check를 로그로 남기면 운영적 안전망이 된다. 또는 `KakaoProcessor`/`AppleProcessor`가 `MockOAuthProcessor`를 상속하면 안 된다는 사실을 KDoc으로만 명시.
  - 권고 수위: 정보 제공. 추가 안 해도 OK.

- [ ] **`application.yaml` 베이스에 이미 `mock-oauth.enabled: false`가 있어 `application-prod.yaml`의 동일 키는 중복.** — `src/main/resources/application.yaml:7-8`, `application-prod.yaml:35-36`
  - prod 안전을 위해 의도적 명시이므로 OK이지만, dev/local만 override로 두면 가독성이 약간 깔끔.

- [ ] **`UnsupportedOAuthModeException`의 위치가 `AuthenticationException.kt` 같은 파일이라 발견성이 낮다.** — `domain/exception/AuthenticationException.kt:22`
  - 설계서가 명시적으로 “기존 파일에 클래스 추가”로 결정했으므로 위반 아님. 다만 IDE에서 “UnsupportedOAuthMode”로 파일 찾으면 안 잡힌다. KDoc 한 줄 첨부 권장.

- [ ] **`AuthController` 변경이 통합 테스트에서 `@WebMvcTest`로 검증된다.** — `src/test/.../AuthControllerIntegrationTest.kt:39-43`
  - `@WebMvcTest`는 controller layer 슬라이스 테스트라 “SecurityFilterChain matches 동작”/“실제 `@ConditionalOnProperty` 조합 동작”은 검증 불가. 위 Critical의 `@SpringBootTest` 통합 테스트가 도입되면 이 슬라이스 테스트는 그대로 유지해도 좋다 (계층 분리 의미가 있음).

---

## 칭찬

- 마커 인터페이스 `MockOAuthProcessor` 채택은 명시적이고 SOLID 친화적. 설계서의 결정 이유(02_design.md 라인 391-397)가 잘 정리되어 있고, `partition` + `associateBy` 조합으로 silent duplicate 키 사고 가능성을 사전에 차단한 점이 견고.
- prod-safe default(`useMock=false`)와 “2차 방어선(런타임 검사) → 400 reject”의 이중 안전 구조가 깔끔. 02_design.md 라인 369-379의 보안 분석이 구체적.
- `OAuthProcessorFactoryTest` 8개 케이스가 4분기 + partition 견고성 + prod 시나리오 + mock 우회 불가 + APPLE/KAKAO 양쪽 커버까지 빠짐없이 검증. 단위 테스트 품질은 매우 좋다.
- `application.yaml`/`application-prod.yaml`에 `mock-oauth.enabled=false`가 중첩 명시되어 “명시가 안전” 원칙을 지키고 있음.
- 03_implementation.md가 “설계 대비 변경 사항: 없음”을 명시하며 설계 충실도를 유지.

---

## implementer에게 전달

1. **Critical 1번**: develop에서 새 브랜치 `feature/#XXX-mock-oauth-header-dispatch`(이슈 번호 부여 후)로 분기하고, colorCode 관련 8 파일 + 테스트 4건은 본 PR에서 제외할 것. 직전 PR #311 사고 패턴이 재현되고 있음. `_workspace_prev_colorcode/`는 정리.
2. **Critical 2번**: `mock-oauth.enabled=false` 시나리오를 진짜 `@SpringBootTest` + `@TestPropertySource(properties = ["mock-oauth.enabled=false"])` 통합 테스트로 추가. 본 PR의 핵심 안전 장치 검증이 빠져 있으니 implementer 또는 test-engineer에게 재할당.
3. **Major 2-3번**: invalid 헤더 값 케이스 통합 테스트 추가, 또는 `String?` 받기로 단순화. `AuthService.login`의 default `useMock = false` 제거 권장.
4. Critical/Major 수정 후 03_implementation.md / 04_test_summary.md 갱신.
