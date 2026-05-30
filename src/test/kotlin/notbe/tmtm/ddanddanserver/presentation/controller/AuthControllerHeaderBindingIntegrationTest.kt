package notbe.tmtm.ddanddanserver.presentation.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import notbe.tmtm.ddanddanserver.application.processor.AppleProcessor
import notbe.tmtm.ddanddanserver.application.processor.KakaoProcessor
import notbe.tmtm.ddanddanserver.application.processor.MockAppleProcessor
import notbe.tmtm.ddanddanserver.application.processor.MockKakaoProcessor
import notbe.tmtm.ddanddanserver.application.processor.OAuthProcessorFactory
import notbe.tmtm.ddanddanserver.application.service.AuthService
import notbe.tmtm.ddanddanserver.application.service.PetCatalogService
import notbe.tmtm.ddanddanserver.common.JWTTokenProvider
import notbe.tmtm.ddanddanserver.common.WebExceptionHandler
import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType
import notbe.tmtm.ddanddanserver.infrastructure.api.AppleAuthApi
import notbe.tmtm.ddanddanserver.infrastructure.api.KakaoAuthApi
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.AuthRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import notbe.tmtm.ddanddanserver.presentation.dto.request.LoginRequest
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Optional

/**
 * `@RequestHeader(... defaultValue="false") useMock: Boolean` 바인딩 동작 실증 테스트 (Major 2).
 *
 * **검증 의도:**
 * - Spring `WebDataBinder`의 기본 Boolean 컨버터가 다양한 문자열 입력에 대해 어떻게 동작하는지 lock-down.
 * - 설계서 02_design.md 라인 216-220 ("1 → false, true → true, 빈/기타 → false") 주장과 실제 동작을 대조.
 * - 04_test_summary.md에 결과를 표로 정리해 향후 회귀 방지.
 *
 * **환경:** `mock-oauth.enabled=true` — Mock 빈이 등록되어 mock 분기 도달을 외부 API 호출 부재로 확정 가능.
 * Real 분기로 가면 `KakaoAuthApi`(mockk relaxed=true)가 호출되어 응답이 다름. 두 분기를 외부 API 호출 횟수로 구분.
 *
 * **컨텍스트 캐시 분리 목적**으로 `AuthControllerProdModeIntegrationTest`(mock-oauth.enabled=false)와 별도 클래스.
 */
@WebMvcTest(
    controllers = [AuthController::class],
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@TestPropertySource(properties = ["mock-oauth.enabled=true"])
@Import(
    value = [
        AuthService::class,
        OAuthProcessorFactory::class,
        KakaoProcessor::class,
        AppleProcessor::class,
        MockKakaoProcessor::class,
        MockAppleProcessor::class,
        WebExceptionHandler::class,
        AuthControllerHeaderBindingIntegrationTest.TestConfig::class,
    ],
)
class AuthControllerHeaderBindingIntegrationTest {
    @TestConfiguration
    class TestConfig {
        @Bean
        fun kakaoAuthApi(): KakaoAuthApi = mockk(relaxed = true)

        @Bean
        fun appleAuthApi(): AppleAuthApi = mockk(relaxed = true)

        @Bean
        fun authRepository(): AuthRepository = mockk(relaxed = true)

        @Bean
        fun userRepository(): UserRepository = mockk(relaxed = true)

        @Bean
        fun jwtTokenProvider(): JWTTokenProvider {
            val mock = mockk<JWTTokenProvider>()
            every { mock.createAccessToken(any()) } returns "access-token-stub"
            every { mock.createRefreshToken(any()) } returns "refresh-token-stub"
            return mock
        }

        @Bean
        fun applicationEventPublisher(): ApplicationEventPublisher = mockk(relaxed = true)

        // AppleProcessor가 ObjectMapper 의존 — Spring Boot 기본 auto-configured ObjectMapper(Kotlin 모듈 포함) 사용.
        // (custom @Bean으로 ObjectMapper override하면 WebMvc 메시지 컨버터 Kotlin data class 역직렬화가 깨진다.)

        @Bean
        fun petCatalogService(): PetCatalogService = mockk(relaxed = true)
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var kakaoAuthApi: KakaoAuthApi

    @Autowired
    lateinit var authRepository: AuthRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @BeforeEach
    fun resetMocks() {
        // Spring 컨텍스트가 캐시되어 mock 빈은 테스트 간 공유된다.
        // 각 테스트가 독립적이도록 호출 기록을 초기화 (answers는 유지).
        clearMocks(kakaoAuthApi, answers = false, recordedCalls = true, childMocks = false)
        clearMocks(authRepository, answers = false, recordedCalls = true, childMocks = false)
        clearMocks(userRepository, answers = false, recordedCalls = true, childMocks = false)
    }

    private fun loginRequestBody(): String =
        objectMapper.writeValueAsString(
            LoginRequest(
                token = "test-token",
                tokenType = OAuthType.KAKAO,
                deviceToken = "device-token",
            ),
        )

    /**
     * 신규 가입 흐름 stub: AuthRepository.findByOAuthIdAndType returns null → 신규 → save/event
     * Mock 분기로 가면 KakaoAuthApi는 호출되지 않고, MockKakaoProcessor가 직접 OAuth를 반환.
     */
    private fun stubNewUserFlow() {
        every { authRepository.findByOAuthIdAndType(any(), any()) } returns null
        every { userRepository.save(any()) } answers { firstArg() }
        every { authRepository.save(any()) } answers { firstArg() }
        every { userRepository.findById(any<ObjectId>()) } returns Optional.empty()
    }

    @Test
    fun `X-Mock-OAuth_true 헤더는 useMock=true로 바인딩되어 mock processor 분기로 진입한다`() {
        stubNewUserFlow()

        mockMvc
            .perform(
                post("/v1/auth/login")
                    .header("X-Mock-OAuth", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequestBody()),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.user.name").value("MockKakao_test-token"))

        // Mock 분기 확정 — Real KakaoAuthApi는 호출되지 않음
        verify(exactly = 0) { kakaoAuthApi.getUserInfo(any()) }
    }

    @Test
    fun `X-Mock-OAuth_false 헤더는 useMock=false로 바인딩되어 real processor 분기로 진입한다`() {
        // Real 분기로 가면 KakaoAuthApi가 호출됨 (relaxed mock이라 응답은 default)
        // KakaoUserInfoResponse가 default일 때 id가 "" → KakaoParseError → 400 KO001
        stubNewUserFlow()

        mockMvc
            .perform(
                post("/v1/auth/login")
                    .header("X-Mock-OAuth", "false")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequestBody()),
            )

        // Real 분기 확정 — KakaoAuthApi 호출됨
        verify(exactly = 1) { kakaoAuthApi.getUserInfo(any()) }
    }

    @Test
    fun `헤더 없음은 defaultValue=false로 바인딩되어 real processor 분기로 진입한다`() {
        stubNewUserFlow()

        mockMvc
            .perform(
                post("/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequestBody()),
            )

        verify(exactly = 1) { kakaoAuthApi.getUserInfo(any()) }
    }

    @Test
    fun `X-Mock-OAuth_TRUE 대문자는 Spring Boolean 컨버터에 의해 true로 바인딩된다 (Boolean_parseBoolean 동작)`() {
        stubNewUserFlow()

        val result =
            mockMvc
                .perform(
                    post("/v1/auth/login")
                        .header("X-Mock-OAuth", "TRUE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestBody()),
                ).andReturn()

        // Mock으로 분기됐다면 KakaoAuthApi 호출 0, Real로 분기됐다면 1.
        // Spring 기본 컨버터(StringToBooleanConverter)는 "TRUE"/"true"/"on"/"yes"/"1"을 true로 인식.
        // → "TRUE" → true 예상 → mock 분기 → KakaoAuthApi 호출 0
        val kakaoCallCount =
            try {
                verify(exactly = 0) { kakaoAuthApi.getUserInfo(any()) }
                0
            } catch (e: AssertionError) {
                1
            }
        // 결과 기록용 — 실제 동작 확인
        println("[X-Mock-OAuth=TRUE] status=${result.response.status}, kakaoAuthApi 호출 횟수=$kakaoCallCount")

        // Spring StringToBooleanConverter는 "TRUE" → true (대소문자 무시) — 이를 lock-down
        kakaoCallCount shouldBe 0
    }

    @Test
    fun `X-Mock-OAuth_1 숫자는 Spring Boolean 컨버터에 의해 true로 바인딩된다`() {
        stubNewUserFlow()

        val result =
            mockMvc
                .perform(
                    post("/v1/auth/login")
                        .header("X-Mock-OAuth", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestBody()),
                ).andReturn()

        val kakaoCallCount =
            try {
                verify(exactly = 0) { kakaoAuthApi.getUserInfo(any()) }
                0
            } catch (e: AssertionError) {
                1
            }
        println("[X-Mock-OAuth=1] status=${result.response.status}, kakaoAuthApi 호출 횟수=$kakaoCallCount")

        // Spring StringToBooleanConverter: "1" → true → mock 분기 → KakaoAuthApi 호출 0
        kakaoCallCount shouldBe 0
    }

    @Test
    fun `X-Mock-OAuth_0 숫자는 false로 바인딩된다`() {
        stubNewUserFlow()

        mockMvc
            .perform(
                post("/v1/auth/login")
                    .header("X-Mock-OAuth", "0")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequestBody()),
            )

        // "0" → false → real 분기 → KakaoAuthApi 호출
        verify(exactly = 1) { kakaoAuthApi.getUserInfo(any()) }
    }

    @Test
    fun `X-Mock-OAuth_yes 문자열은 Spring Boolean 컨버터에 의해 true로 바인딩된다`() {
        stubNewUserFlow()

        val result =
            mockMvc
                .perform(
                    post("/v1/auth/login")
                        .header("X-Mock-OAuth", "yes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestBody()),
                ).andReturn()

        val kakaoCallCount =
            try {
                verify(exactly = 0) { kakaoAuthApi.getUserInfo(any()) }
                0
            } catch (e: AssertionError) {
                1
            }
        println("[X-Mock-OAuth=yes] status=${result.response.status}, kakaoAuthApi 호출 횟수=$kakaoCallCount")

        // Spring StringToBooleanConverter: "yes" → true → mock 분기
        kakaoCallCount shouldBe 0
    }

    @Test
    fun `X-Mock-OAuth_garbage 임의 문자열은 400 INVALID_INPUT으로 거절된다`() {
        // Spring StringToBooleanConverter는 인식되지 않는 값에 IllegalArgumentException
        // → MethodArgumentTypeMismatchException → WebExceptionHandler.handleInvalidInput → 400 DE0003
        mockMvc
            .perform(
                post("/v1/auth/login")
                    .header("X-Mock-OAuth", "garbage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequestBody()),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("DE0003"))

        // 바인딩 자체에서 400 → 서비스/외부 API 도달 안 함
        verify(exactly = 0) { kakaoAuthApi.getUserInfo(any()) }
    }
}
