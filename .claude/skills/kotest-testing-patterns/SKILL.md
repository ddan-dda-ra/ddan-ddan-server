---
name: kotest-testing-patterns
description: ddan-ddan-server의 테스트 작성 가이드. Kotest 5.9.1 FunSpec + MockK 1.13.11 조합, beforeEach 초기화, 한국어 테스트명, shouldBe/shouldThrow assertion DSL을 다룬다. 모든 테스트 코드 작성/수정 시 반드시 참조 — 이 프로젝트는 JUnit 어노테이션 스타일이 아닌 Kotest FunSpec 스타일을 표준으로 사용한다.
---

# Kotest Testing Patterns (ddan-ddan-server)

## 의존성

```kotlin
testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
testImplementation("io.kotest:kotest-assertions-core:5.9.1")
testImplementation("io.mockk:mockk:1.13.11")
```

- **Spec 스타일은 `FunSpec` 단일 표준** — 이 프로젝트는 `BehaviorSpec`/`StringSpec` 등 다른 스타일을 사용하지 않는다.
- 빌드 도구는 JUnit Platform 위에서 실행됨 (`useJUnitPlatform()`).
- 테스트 실행: `./gradlew test` 또는 `./gradlew test --tests "...패키지..."`

## 표준 테스트 골격

```kotlin
class FooServiceTest : FunSpec({
    lateinit var fooRepository: FooRepository
    lateinit var barClient: BarClient
    lateinit var fooService: FooService

    beforeEach {
        fooRepository = mockk()
        barClient = mockk(relaxed = true)
        fooService = FooService(fooRepository, barClient)
    }

    test("정상 케이스 - 한국어로 명확하게 기술") {
        // given
        every { fooRepository.findByX(any()) } returns aFoo()

        // when
        val result = fooService.doSomething(...)

        // then
        result.someField shouldBe expected
        verify { barClient.send(any()) }
    }

    test("예외 케이스 - 무엇이 없을 때 무엇이 던져진다") {
        every { fooRepository.findByX(any()) } throws FooNotFoundException()

        shouldThrow<FooNotFoundException> {
            fooService.doSomething(...)
        }
    }
})
```

## 핵심 규칙

### 1. 테스트 이름은 한국어로

`test("...")` 안에 한국어 문장으로 시나리오를 기술한다. 영어 함수명·camelCase 금지.

좋음: `test("응원을 생성할 수 있다")`, `test("중복 응원 시도 시 예외가 발생한다")`
나쁨: `test("createCheer success")`, `test("should throw when duplicate")`

### 2. `beforeEach`로 mock 초기화

각 테스트는 독립적이어야 한다. `beforeEach`에서 mock과 SUT를 새로 만든다.

- 클래스 레벨에 mock을 두면 테스트 간 상태가 누설된다 — `lateinit var` + `beforeEach` 패턴 고수
- `afterEach`로 `clearAllMocks()` 호출은 보통 불필요 (재할당으로 충분)

### 3. assertion은 Kotest DSL

```kotlin
result shouldBe expected           // ==
result shouldNotBe other
result shouldContain "..."
result.shouldBeInstanceOf<Foo>()
list shouldHaveSize 3
shouldThrow<XException> { ... }
```

- `assertEquals`, `assertTrue` 등 JUnit assertion 사용 금지

### 4. MockK 패턴

```kotlin
val repo = mockk<FooRepository>()
val pushClient = mockk<PushClient>(relaxed = true)   // 반환값 신경 안 쓸 때

every { repo.findByIdOrThrow(id) } returns aFoo()
every { repo.save(any()) } returns savedFoo
every { repo.delete(any()) } throws SomeException()

verify { repo.save(any()) }
verify(exactly = 1) { pushClient.sendToUser(any(), any(), any()) }
verify(exactly = 0) { pushClient.sendBroadcast(any()) }   // 호출되지 않음을 검증
```

- `relaxed = true`는 반환값/Unit 함수에 stub을 안 줘도 되게 해준다 — 외부 클라이언트(`PushClient`, `KakaoApiClient` 등) mock에 자주 사용
- `mockkStatic`/`mockkObject`는 가급적 피하고, 정 필요하면 `beforeEach` + `afterEach`에 짝으로 등록

### 5. 도메인 객체 헬퍼

`TestUserGenerator`처럼 보일러플레이트를 줄이는 헬퍼가 이미 존재한다. 새 헬퍼를 만들기 전에 `src/test/kotlin/.../TestUserGenerator.kt` 등을 먼저 확인.

```kotlin
val user = TestUserGenerator.create(name = "테스트유저")
```

## 통합 테스트 (Spring 컨텍스트)

```kotlin
@SpringBootTest
class FooIntegrationTest(
    private val fooService: FooService,   // 또는 @Autowired
) : FunSpec({
    test("...") { ... }
})
```

- 단위 테스트로 충분하면 단위로 (대부분의 service)
- Repository 통합 테스트는 `@DataMongoTest` 활용 (이 레포의 기존 `*RepositoryImplTest` 참조)
- Spring 컨텍스트 띄우는 통합 테스트는 비싸므로 신중히

## Spring 어노테이션 핵심 코드는 통합 테스트 필수

다음 어노테이션이 **동작의 핵심**인 코드는 **단위 테스트만으로는 절대 검증 불가** — 반드시 통합 테스트를 함께 작성한다:

- `@Transactional`, `@TransactionalEventListener`
- `@Async`, `@Scheduled`
- `@EventListener`, `@Cacheable`
- 그 외 Spring AOP가 처리하는 모든 어드바이스

### 왜 단위 테스트로 안 되는가

위 어노테이션들은 모두 **Spring AOP 프록시**가 처리한다. 테스트에서 `MyListener()`로 인스턴스를 직접 만들고 `listener.handle(event)`로 메서드를 직접 호출하면 프록시를 우회하므로 **어노테이션이 모두 무시되어 일반 함수 호출이 된다.** 메서드 본문 로직만 검증되고 어노테이션 동작(트랜잭션 경계, 비동기 실행, 이벤트 phase, fallbackExecution 등)은 0% 검증된다.

### 실패 사례 (PR #277)

`@TransactionalEventListener(phase = AFTER_COMMIT)`을 썼는데 단일 MongoDB 환경(`MongoTransactionManager` 미등록)에서 `@Transactional`이 NO-OP이 되어 listener가 silent하게 dropping. 단위 테스트는 listener를 직접 호출했기에 통과했고, 통합 테스트가 부재해 dev 배포 후에야 발견. `fallbackExecution = true` 추가로 핫픽스. (참조: `_workspace/05_review.md` 또는 git log)

### 통합 테스트 패턴 — `@SpringJUnitConfig` 슬라이스

`@SpringBootTest` 풀로 컨텍스트 띄우는 비용이 부담스러우면, 필요한 빈만 등록하는 좁은 슬라이스를 쓴다:

```kotlin
@SpringJUnitConfig(
    classes = [
        UserRegisteredEventListener::class,
        UserRegisteredEventListenerIntegrationTest.TestConfig::class,
    ]
)
@TestPropertySource(properties = ["spring.profiles.active=test"])
class UserRegisteredEventListenerIntegrationTest {

    @TestConfiguration
    class TestConfig {
        @Bean fun userRepository(): UserRepository = mockk()
        @Bean fun discordHookApi(): DiscordHookApi = mockk(relaxed = true)
    }

    @Autowired lateinit var publisher: ApplicationEventPublisher
    @Autowired lateinit var discordHookApi: DiscordHookApi

    @Test
    fun `이벤트 발행 시 listener가 실제로 호출된다`() {
        every { /* ... */ } returns /* ... */
        publisher.publishEvent(SomeEvent(...))
        verify { discordHookApi.sendMessage(any()) }
    }
}
```

핵심:
- **`@SpringJUnitConfig`**(JUnit5)로 좁은 컨텍스트, classes에 필요한 빈만 명시
- 외부 의존(MongoDB, FCM, OAuth API 등)은 `@TestConfiguration` 안에서 `mockk()`로 빈 등록
- `ApplicationEventPublisher` 주입 → `publishEvent` 호출 → mock이 호출됐는지 `verify`
- **Kotest FunSpec이 아닌 JUnit5 형태 사용 OK** — 이 프로젝트는 Kotest Spring extension 의존성이 없음. 통합 테스트 한 케이스를 위해 의존성 추가하기보다 JUnit5로 작성하는 게 가벼움.

### 단위 테스트 + 통합 테스트 분담

| 테스트 유형 | 검증 대상 |
|---|---|
| 단위 테스트 (FunSpec, listener 직접 호출) | 메서드 본문 로직 (메시지 포맷, 분기, 예외 흡수) |
| 통합 테스트 (@SpringJUnitConfig, publisher 호출) | 어노테이션 동작 (이벤트 wiring, fallbackExecution, 비동기 dispatch) |

둘 다 작성한다. 단위만 있으면 함정에 빠진다.

## 테스트 위치

production 코드의 패키지 구조를 그대로 미러링한다:

```
src/main/kotlin/notbe/tmtm/ddanddanserver/application/service/CheerService.kt
src/test/kotlin/notbe/tmtm/ddanddanserver/application/service/CheerServiceTest.kt
```

## 커버리지 목표 (필수 × 권장)

- **필수**: 정상 흐름 + 주요 예외 흐름 (도메인 예외 분기 모두)
- 권장: 경계 케이스 (빈 목록, null 가능 필드, 동시성 시나리오), 외부 의존 실패 시 동작
- "한 줄도 못 짠 변경"은 없도록 — 트리비얼한 DTO 변환에는 단위 테스트 생략 가능

## 자주 만나는 문제

| 증상 | 원인 / 해결 |
|---|---|
| `MockK could not match` | `every`의 인자가 `any()` 또는 정확히 일치하는지 확인 |
| 테스트 간 상태 누설 | `beforeEach`에서 mock과 SUT를 새로 할당하는지 확인 |
| `Test runner` 인식 안됨 | `kotest-runner-junit5` 의존이 testImplementation에 있는지 |
| 통합 테스트가 느림 | 단위 테스트로 가능한지 재검토. 정 필요하면 `@DataMongoTest`로 좁힘 |
