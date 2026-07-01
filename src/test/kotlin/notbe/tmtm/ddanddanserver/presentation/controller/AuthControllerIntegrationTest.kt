package notbe.tmtm.ddanddanserver.presentation.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import notbe.tmtm.ddanddanserver.application.service.AuthService
import notbe.tmtm.ddanddanserver.domain.model.auth.AuthResult
import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType
import notbe.tmtm.ddanddanserver.domain.model.user.DeviceToken
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.presentation.dto.request.LoginRequest
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * AuthController 통합 테스트 (controller → service 인자 전파 검증).
 *
 * - `@RequestHeader(required = false, defaultValue = "false") useMock: Boolean` 바인딩이 controller→service에 정확히 전파되는지 확인.
 * - 헤더가 없거나 false일 때, 그리고 true일 때 service.login의 useMock 파라미터로 전달되는지 검증.
 *
 * **분담:**
 * - 실제 빈 컨텍스트 + `@ConditionalOnProperty` + factory partition + WebExceptionHandler 흐름 검증은
 *   `AuthControllerProdModeIntegrationTest`(mock-oauth.enabled=false)와
 *   `AuthControllerHeaderBindingIntegrationTest`(mock-oauth.enabled=true)에서 담당.
 * - 본 테스트는 service mock 슬라이스로 controller layer만 격리해 검증.
 */
@WebMvcTest(
    controllers = [AuthController::class],
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@Import(AuthControllerIntegrationTest.TestConfig::class)
class AuthControllerIntegrationTest {
    @TestConfiguration
    class TestConfig {
        @Bean
        fun authService(): AuthService = mockk(relaxed = true)

    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var authService: AuthService

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private fun user(name: String = "테스트유저"): User =
        User(
            id = ObjectId(),
            deviceToken = DeviceToken("device-token"),
            name = name,
        )

    private fun authResult(name: String = "테스트유저"): AuthResult =
        AuthResult(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            user = user(name),
        )

    private fun loginRequestBody(): String =
        objectMapper.writeValueAsString(
            LoginRequest(
                token = "kakao-access-token",
                tokenType = OAuthType.KAKAO,
                deviceToken = "device-token",
            ),
        )

    @Test
    fun `X-Mock-OAuth 헤더가 없으면 useMock=false로 service에 전달된다`() {
        val useMockSlot = slot<Boolean>()
        every { authService.login(any(), any(), any(), capture(useMockSlot)) } returns authResult()

        mockMvc
            .perform(
                post("/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequestBody()),
            ).andExpect(status().isOk)

        useMockSlot.captured shouldBe false
        verify(exactly = 1) { authService.login(any(), any(), any(), false) }
    }

    @Test
    fun `X-Mock-OAuth=false 헤더가 들어오면 useMock=false로 service에 전달된다`() {
        val useMockSlot = slot<Boolean>()
        every { authService.login(any(), any(), any(), capture(useMockSlot)) } returns authResult()

        mockMvc
            .perform(
                post("/v1/auth/login")
                    .header("X-Mock-OAuth", "false")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequestBody()),
            ).andExpect(status().isOk)

        useMockSlot.captured shouldBe false
    }

    @Test
    fun `X-Mock-OAuth=true 헤더가 들어오면 useMock=true로 service에 전달된다`() {
        val useMockSlot = slot<Boolean>()
        every { authService.login(any(), any(), any(), capture(useMockSlot)) } returns authResult("목유저")

        mockMvc
            .perform(
                post("/v1/auth/login")
                    .header("X-Mock-OAuth", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequestBody()),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.user.name").value("목유저"))

        useMockSlot.captured shouldBe true
        verify(exactly = 1) { authService.login(any(), any(), any(), true) }
    }

    // service에서 UnsupportedOAuthModeException이 발생하는 케이스는 service-level mocking으로 시뮬레이션하는 것이
    // 실제 prod 시나리오(`mock-oauth.enabled=false`+mock 빈 미등록) 흐름을 통과 검증하지 못해
    // `AuthControllerProdModeIntegrationTest`로 이관됨 (05_review.md Critical 2 + Major 1).
}
