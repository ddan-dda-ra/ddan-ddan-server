package notbe.tmtm.ddanddanserver.config

import io.mockk.every
import io.mockk.mockk
import notbe.tmtm.ddanddanserver.application.service.AuthService
import notbe.tmtm.ddanddanserver.application.service.PetCatalogService
import notbe.tmtm.ddanddanserver.common.JWTTokenProvider
import notbe.tmtm.ddanddanserver.common.WebExceptionHandler
import notbe.tmtm.ddanddanserver.domain.model.auth.AuthResult
import notbe.tmtm.ddanddanserver.domain.model.user.DeviceToken
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.presentation.controller.AuthController
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.logging.LoggersEndpointAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.web.mappings.MappingsEndpointAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.web.servlet.ServletManagementContextAutoConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * WebSecurityConfig 통합 테스트 — 실제 SecurityFilterChain 3개 + actuator 엔드포인트를 MockMvc로 검증.
 *
 * **검증 의도:**
 * - `/v1`, `/v1/auth` 체인에 매칭되지 않는 경로는 fallbackFilterChain이 기본 거부한다 (actuator 외부 노출 차단).
 * - 프로필 설정 실수로 `loggers`/`mappings`가 노출되더라도 보안 체인에서 막히는지 확인하기 위해
 *   테스트에서는 의도적으로 해당 엔드포인트를 노출한다.
 * - `/actuator/health`는 홈서버 헬스체크용으로 익명 접근이 가능해야 한다.
 * - fallback 체인 추가 후에도 `/v1/auth/login`은 차단되지 않고, `/v1` 하위 API는 여전히 인증이 필요하다 (PR #302 회귀 방지).
 *
 * **`@WebMvcTest` 선택 이유:** 임베디드 MongoDB가 없어 `@SpringBootTest` 풀 컨텍스트 부팅이 불가하므로
 * `WebSecurityConfig`와 actuator auto-config를 슬라이스에 직접 import한다.
 */
@WebMvcTest(controllers = [AuthController::class])
@TestPropertySource(properties = ["management.endpoints.web.exposure.include=health,loggers,mappings"])
@ImportAutoConfiguration(
    EndpointAutoConfiguration::class,
    WebEndpointAutoConfiguration::class,
    HealthContributorAutoConfiguration::class,
    HealthEndpointAutoConfiguration::class,
    LoggersEndpointAutoConfiguration::class,
    MappingsEndpointAutoConfiguration::class,
    ManagementContextAutoConfiguration::class,
    ServletManagementContextAutoConfiguration::class,
)
@EnableConfigurationProperties(AppVersionProperties::class)
@Import(
    value = [
        WebSecurityConfig::class,
        WebExceptionHandler::class,
        WebSecurityConfigIntegrationTest.TestConfig::class,
    ],
)
class WebSecurityConfigIntegrationTest {
    @TestConfiguration
    class TestConfig {
        @Bean
        fun authService(): AuthService =
            mockk(relaxed = true) {
                every { login(any(), any(), any(), any()) } returns
                    AuthResult(
                        accessToken = "access-token",
                        refreshToken = "refresh-token",
                        user = User(id = ObjectId(), deviceToken = DeviceToken("device-token"), name = "테스트유저"),
                    )
            }

        @Bean
        fun petCatalogService(): PetCatalogService = mockk(relaxed = true)

        @Bean
        fun jwtTokenProvider(): JWTTokenProvider = mockk(relaxed = true)
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `actuator health는 인증 없이 200을 반환한다`() {
        mockMvc
            .perform(get("/actuator/health"))
            .andExpect(status().isOk)
    }

    @Test
    fun `actuator loggers는 엔드포인트가 노출되어 있어도 익명 요청을 403으로 거부한다`() {
        mockMvc
            .perform(get("/actuator/loggers"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `actuator loggers 로그 레벨 변경 요청은 403으로 거부한다`() {
        mockMvc
            .perform(
                post("/actuator/loggers/ROOT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"configuredLevel":"TRACE"}"""),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `actuator mappings는 익명 요청을 403으로 거부한다`() {
        mockMvc
            .perform(get("/actuator/mappings"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `어떤 체인에도 매칭되지 않는 임의 경로는 403으로 거부한다`() {
        mockMvc
            .perform(get("/unknown-path"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `fallback 체인 추가 후에도 v1 auth login은 인증 없이 통과한다`() {
        mockMvc
            .perform(
                post("/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"token":"kakao-access-token","tokenType":"KAKAO","deviceToken":"device-token"}"""),
            ).andExpect(status().isOk)
    }

    @Test
    fun `v1 엔드포인트는 토큰 없이 요청하면 401을 반환한다`() {
        mockMvc
            .perform(get("/v1/users/me"))
            .andExpect(status().isUnauthorized)
    }
}
