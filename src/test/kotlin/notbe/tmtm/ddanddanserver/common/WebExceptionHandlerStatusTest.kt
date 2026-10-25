package notbe.tmtm.ddanddanserver.common

import io.mockk.mockk
import notbe.tmtm.ddanddanserver.application.service.AuthService
import notbe.tmtm.ddanddanserver.presentation.controller.AuthController
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * WebExceptionHandler가 경로 오류와 메서드 오류를 서로 다른 상태 코드로 응답하는지 MVC 슬라이스로 확인한다.
 */
@WebMvcTest(
    controllers = [AuthController::class],
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@Import(WebExceptionHandlerStatusTest.TestConfig::class)
class WebExceptionHandlerStatusTest {
    @TestConfiguration
    class TestConfig {
        @Bean
        fun authService(): AuthService = mockk(relaxed = true)
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `지원하지 않는 HTTP 메서드는 405와 DE0005를 응답한다`() {
        mockMvc.perform(get("/v1/auth/login"))
            .andExpect(status().isMethodNotAllowed)
            .andExpect(jsonPath("$.code").value("DE0005"))
    }

    @Test
    fun `없는 경로는 404와 DE0004를 응답한다`() {
        mockMvc.perform(get("/v1/not-exist"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("DE0004"))
    }
}
