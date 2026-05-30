package notbe.tmtm.ddanddanserver.presentation.controller

import com.fasterxml.jackson.databind.ObjectMapper
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
import notbe.tmtm.ddanddanserver.infrastructure.api.KakaoAuthApi
import notbe.tmtm.ddanddanserver.infrastructure.api.AppleAuthApi
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.AuthRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import notbe.tmtm.ddanddanserver.presentation.dto.request.LoginRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.client.HttpClientErrorException

/**
 * Prod 시나리오 통합 테스트 — `mock-oauth.enabled=false`.
 *
 * **검증 의도 (Critical 2 + Major 1):**
 * - `@ConditionalOnProperty(... havingValue="true")`가 `MockKakaoProcessor`/`MockAppleProcessor`에서 평가되어
 *   prod 환경에서는 **mock 빈이 등록되지 않음**을 실제 컨텍스트로 확인.
 * - 이 상태에서 `OAuthProcessorFactory.partition`이 mock 풀을 비운 채로 동작하며,
 *   `X-Mock-OAuth: true` 요청이 들어오면 `UnsupportedOAuthModeException`이 throw → `WebExceptionHandler`가
 *   400 + `AC006`을 반환하는 흐름을 service mocking 없이 검증.
 * - 부모 `AuthenticationException`(401)보다 자식 `UnsupportedOAuthModeException`(400) 핸들러가 우선 매칭됨을
 *   Spring MVC 전체 dispatcher 흐름으로 동시 검증 (Major 1).
 *
 * **컨텍스트 캐시 분리 목적**으로 `mock-oauth.enabled=true`/`false` 별도 클래스로 분리.
 *
 * **`@WebMvcTest` 선택 이유:** 이 코드베이스에는 임베디드 MongoDB가 없어 `@SpringBootTest` 풀 컨텍스트가
 * MongoDB 부재로 부팅 실패한다. `@WebMvcTest` + `@Import`로 (a) MockMvc auto-config, (b) `@ConditionalOnProperty`
 * 평가, (c) 실제 `OAuthProcessorFactory.partition` 동작, (d) `WebExceptionHandler` 매처 평가를 모두 만족.
 * `@AutoConfigureMockMvc`+`@SpringBootTest`와 동등한 검증 범위를 슬라이스로 달성.
 */
@WebMvcTest(
    controllers = [AuthController::class],
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@TestPropertySource(properties = ["mock-oauth.enabled=false"])
@Import(
    value = [
        AuthService::class,
        OAuthProcessorFactory::class,
        KakaoProcessor::class,
        AppleProcessor::class,
        MockKakaoProcessor::class,
        MockAppleProcessor::class,
        WebExceptionHandler::class,
        AuthControllerProdModeIntegrationTest.TestConfig::class,
    ],
)
class AuthControllerProdModeIntegrationTest {
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
        fun jwtTokenProvider(): JWTTokenProvider = mockk(relaxed = true)

        @Bean
        fun applicationEventPublisher(): ApplicationEventPublisher = mockk(relaxed = true)

        // AppleProcessor가 ObjectMapper 의존 — Spring Boot 기본 auto-configured ObjectMapper를
        // 그대로 사용하되, Jackson 등록을 위해 Kotlin 모듈만 추가.
        // (custom @Bean으로 ObjectMapper를 override하면 WebMvc 메시지 컨버터에도 영향이 가서
        // LoginRequest 같은 Kotlin data class 역직렬화가 깨진다.)

        // PetCatalogVersionFilter가 글로벌이라 컨텍스트에 필요
        @Bean
        fun petCatalogService(): PetCatalogService = mockk(relaxed = true)
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var oAuthProcessorFactory: OAuthProcessorFactory

    @Autowired
    lateinit var kakaoAuthApi: KakaoAuthApi

    @BeforeEach
    fun resetMocks() {
        // Spring 컨텍스트가 캐시되어 mock 빈은 테스트 간 공유된다.
        // 각 테스트가 독립적이도록 호출 기록을 초기화.
        clearMocks(kakaoAuthApi, answers = false, recordedCalls = true, childMocks = false)
    }

    private fun loginRequestBody(type: OAuthType = OAuthType.KAKAO): String =
        objectMapper.writeValueAsString(
            LoginRequest(
                token = "test-access-token",
                tokenType = type,
                deviceToken = "device-token",
            ),
        )

    @Test
    fun `mock-oauth_enabled=false 환경에서 X-Mock-OAuth=true KAKAO 요청은 400 + AC006 + data=KAKAO를 반환한다 (Critical 2 + Major 1)`() {
        // Real 카카오 API 호출이 발생하면 안 됨 — 그 전에 factory가 UnsupportedOAuthModeException을 던져야 함

        mockMvc
            .perform(
                post("/v1/auth/login")
                    .header("X-Mock-OAuth", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequestBody(OAuthType.KAKAO)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("AC006"))
            .andExpect(jsonPath("$.data").value("KAKAO"))

        // mock으로 분기됐으므로 real KakaoAuthApi는 호출되지 않아야 함
        verify(exactly = 0) { kakaoAuthApi.getUserInfo(any()) }
    }

    @Test
    fun `mock-oauth_enabled=false 환경에서 X-Mock-OAuth=true APPLE 요청은 400 + AC006 + data=APPLE을 반환한다`() {
        mockMvc
            .perform(
                post("/v1/auth/login")
                    .header("X-Mock-OAuth", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequestBody(OAuthType.APPLE)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("AC006"))
            .andExpect(jsonPath("$.data").value("APPLE"))
    }

    @Test
    fun `mock-oauth_enabled=false 환경에서도 OAuthProcessorFactory의 realMap에는 KAKAO와 APPLE이 모두 등록된다`() {
        // partition 결과 확인: useMock=false로 호출 시 real processor가 정상 반환되어야 한다 (예외 없음)
        val realKakao = oAuthProcessorFactory.getClient(OAuthType.KAKAO, useMock = false)
        val realApple = oAuthProcessorFactory.getClient(OAuthType.APPLE, useMock = false)

        assert(realKakao is KakaoProcessor) { "mock-oauth.enabled=false 환경에서 KakaoProcessor 실제 빈이 반환되어야 한다" }
        assert(realApple is AppleProcessor) { "mock-oauth.enabled=false 환경에서 AppleProcessor 실제 빈이 반환되어야 한다" }
    }

    @Test
    fun `mock-oauth_enabled=false 환경에서 OAuthProcessorFactory_getClient(KAKAO, useMock=true)는 UnsupportedOAuthModeException을 직접 던진다`() {
        // 단위 검증과 동일하지만 SpringContext에서 빈 등록 상태로 확인 — mockMap이 비어있음을 직접 보증
        try {
            oAuthProcessorFactory.getClient(OAuthType.KAKAO, useMock = true)
            assert(false) { "UnsupportedOAuthModeException이 던져졌어야 한다" }
        } catch (e: notbe.tmtm.ddanddanserver.domain.exception.UnsupportedOAuthModeException) {
            assert(e.data == OAuthType.KAKAO) { "data는 KAKAO여야 한다: ${e.data}" }
        }
    }

    @Test
    fun `mock-oauth_enabled=false 환경에서 X-Mock-OAuth=false 헤더 + 외부 API 401 응답이면 OAuth 401 에러로 변환된다`() {
        // Real 카카오 API가 401을 던지면 KakaoUnauthorizedError → 401 AuthenticationException 핸들러로 매핑
        every { kakaoAuthApi.getUserInfo(any()) } throws HttpClientErrorException(HttpStatus.UNAUTHORIZED)

        mockMvc
            .perform(
                post("/v1/auth/login")
                    .header("X-Mock-OAuth", "false")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequestBody(OAuthType.KAKAO)),
            ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("KO003"))

        verify(exactly = 1) { kakaoAuthApi.getUserInfo(any()) }
    }

    @Test
    fun `mock-oauth_enabled=false 환경에서 헤더 없음 요청은 Real processor로 분기되어 외부 API를 호출한다`() {
        // 헤더 없음 → defaultValue=false → real processor 경로 → KakaoAuthApi 호출됨
        every { kakaoAuthApi.getUserInfo(any()) } throws HttpClientErrorException(HttpStatus.UNAUTHORIZED)

        mockMvc
            .perform(
                post("/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequestBody(OAuthType.KAKAO)),
            ).andExpect(status().isUnauthorized) // KakaoUnauthorizedError → 401

        // useMock=false 분기로 갔는지 = Real KakaoAuthApi가 호출됐는지로 확정
        verify(exactly = 1) { kakaoAuthApi.getUserInfo(any()) }
    }
}
