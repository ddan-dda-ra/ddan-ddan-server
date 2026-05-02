---
name: kotlin-spring-conventions
description: ddan-ddan-server의 Kotlin/Spring Boot/MongoDB 코드 컨벤션. 생성자 주입, @Document 엔티티, JWT 인증, WebExceptionHandler 글로벌 예외 처리, LoggingFilter, OpenAPI 문서화 패턴을 다룬다. 새 코드 작성과 코드 리뷰 시 반드시 참조 — 컨벤션 위반은 PR 리뷰의 가장 잦은 지적 사유다.
---

# Kotlin / Spring Boot / MongoDB Conventions (ddan-ddan-server)

## 환경

- Kotlin 2.2.0 (build.gradle.kts), JDK 17 (toolchain)
- Spring Boot 3.5.4
- MongoDB (Spring Data MongoDB)
- 빌드: `./gradlew build`, 테스트: `./gradlew test`, 컴파일만: `./gradlew compileKotlin`

## Kotlin 스타일

### 클래스 / 프로퍼티

- 기본은 `data class` 또는 일반 `class` — `class`에 빈 생성자 금지, 필요한 의존만 받기
- 변경 불가 우선: `val` > `var`. `var`은 명확한 가변 상태일 때만.
- 가시성: 기본 `public` 그대로 두지 않는다 — 외부 노출이 필요 없으면 `internal` 또는 파일/클래스 private
- nullable 남용 금지. `!!`는 마지막 수단.

### 함수

- 한 줄짜리 함수는 `=` 표현식 본문
- 확장 함수는 도메인/유틸리티 명확한 곳에만. 컨트롤러/서비스에 확장 함수 난립 금지.

## Spring DI

```kotlin
@Service
class CheerService(
    private val cheerRepository: CheerRepository,
    private val userRepository: UserRepository,
    private val pushClient: PushClient,
) {
    fun createCheer(...) { ... }
}
```

- **항상 생성자 주입 + `val`** — `@Autowired` 필드 주입 금지
- `@Service`는 application/service/, `@RestController`는 presentation/controller/, `@Component`는 꼭 필요한 인프라 어댑터에만
- 설정 클래스는 `@Configuration` + `@Bean`

## MongoDB 엔티티

```kotlin
@Document(collection = "cheer")
class CheerDocument(
    @Id
    val id: ObjectId = ObjectId(),
    val cheererId: ObjectId,
    val cheereeId: ObjectId,
    val createdAt: Instant = Instant.now(),
)
```

- **엔티티는 반드시 `infrastructure/database/`에만** (도메인은 `Cheer` 등 별도 모델)
- `@Id`는 `ObjectId`. 문자열 ID 변환은 도메인/DTO에서.
- 인덱스가 필요하면 `@Indexed` + 복합 인덱스는 `@CompoundIndex` (또는 `MongoConfig`에 한 곳에 모음 — 기존 패턴 확인)
- Repository: `interface ...Repository : MongoRepository<...>` + 커스텀이 필요하면 `RepositoryCustom`/`RepositoryCustomImpl` 분리

## JWT 인증

- `JWTTokenProvider`에서 토큰 생성/검증
- 사용자 ID 추출은 SecurityContext에서 (필터가 채워줌)
- 인증 필요한 엔드포인트는 자동으로 필터를 통과 — 컨트롤러에서 별도 처리 불필요
- 토큰을 컨트롤러에서 직접 다루지 말고, 추출된 사용자 ID를 받는다

## 예외 처리

### 도메인 예외

```kotlin
package notbe.tmtm.ddanddanserver.domain.exception

class CheerAlreadyExistsException : RuntimeException("...")
```

- 모든 비즈니스 실패는 도메인 예외로
- `WebExceptionHandler`에 매핑이 있어야 클라이언트에 적절한 status + 코드로 응답됨
- **컨트롤러/서비스에서 `try/catch`로 잡지 말 것** — 글로벌 핸들러로 흘려보낸다

### 글로벌 핸들러

`presentation` 또는 `common`에 `@RestControllerAdvice` 클래스 (`WebExceptionHandler`).
- 새 도메인 예외 추가 시 반드시 핸들러에도 매핑 추가

## 컨트롤러 / OpenAPI

```kotlin
@RestController
@RequestMapping("/api/v1/cheer")
@Tag(name = "Cheer", description = "응원하기 API")
class CheerController(private val cheerService: CheerService) {

    @Operation(summary = "응원 생성")
    @ApiResponse(responseCode = "200", description = "성공")
    @PostMapping("/{cheereeId}")
    fun createCheer(
        @AuthenticationPrincipal cheererId: ObjectId,
        @PathVariable cheereeId: ObjectId,
    ): CheerResponse {
        val cheer = cheerService.createCheer(cheererId, cheereeId)
        return CheerResponse.from(cheer)
    }
}
```

- `@Operation`, `@ApiResponse`, `@Parameter`, `@Tag` 적극 사용 (Swagger UI: `/swagger-ui.html`)
- 경로 컨벤션: `/api/v1/{resource}`
- DTO는 `presentation/dto/{feature}/`. 응답은 `from(domain)` 팩토리 메서드 패턴

## 로깅

- `LoggingFilter`가 모든 요청/응답을 로깅
- 추가 로깅은 `LoggingUtils` 사용 (있으면)
- `private val log = LoggerFactory.getLogger(...)` 또는 Kotlin idiom
- **민감 정보(JWT, deviceToken, 사용자 PII)는 절대 로깅 금지**

## FCM (푸시)

- `PushClient`로 추상화되어 있음 — 컨트롤러/서비스에서 Firebase Admin SDK 직접 호출 금지
- 사용자 알림 설정(`UserSetting.isAppPushOn`)을 항상 확인 후 발송

## 스케줄러

- `presentation/scheduler/`에 위치
- `@Scheduled(cron = "...")` + `Asia/Seoul` 타임존 (Docker 컨테이너에 설정됨)
- 멱등성을 보장 — 중복 실행되어도 안전해야 함

## 작은 규칙 모음

- 파일 끝에 항상 개행 (`\n`)
- 한 파일 한 클래스 원칙. 작은 sealed/enum 동봉은 OK.
- import는 IDE 정렬 그대로 (와일드카드 import 지양 — Kotlin은 IDE 기본설정으로 충분)
- `println` 금지 (로깅으로)
- 매직 넘버는 `companion object` const로 추출
