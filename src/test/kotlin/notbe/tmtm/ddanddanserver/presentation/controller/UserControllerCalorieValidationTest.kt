package notbe.tmtm.ddanddanserver.presentation.controller

import io.mockk.mockk
import io.mockk.verify
import notbe.tmtm.ddanddanserver.application.service.DailyInfoService
import notbe.tmtm.ddanddanserver.application.service.UserPetService
import notbe.tmtm.ddanddanserver.application.service.UserProfileService
import notbe.tmtm.ddanddanserver.application.service.UserService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 일일 칼로리 범위 검증 테스트.
 *
 * 검증은 CalorieRequest 생성 시점(Jackson 역직렬화)에 일어나므로, WebExceptionHandler까지 거쳐 400이 나가는지 MVC 슬라이스로 확인한다.
 */
@WebMvcTest(
    controllers = [UserController::class],
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@Import(UserControllerCalorieValidationTest.TestConfig::class)
class UserControllerCalorieValidationTest {
    @TestConfiguration
    class TestConfig {
        @Bean
        fun userService(): UserService = mockk(relaxed = true)

        @Bean
        fun userPetService(): UserPetService = mockk(relaxed = true)

        @Bean
        fun dailyInfoService(): DailyInfoService = mockk(relaxed = true)

        @Bean
        fun userProfileService(): UserProfileService = mockk(relaxed = true)
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var userService: UserService

    @Test
    fun `칼로리가 10000을 넘으면 400을 응답하고 보상을 지급하지 않는다`() {
        patchCalorie(10_001)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("DE0003"))

        verify(exactly = 0) { userService.updateCalorieAndRewardFood(any(), any(), any()) }
    }

    @Test
    fun `칼로리가 음수면 400을 응답한다`() {
        patchCalorie(-1)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("DE0003"))
    }

    private fun patchCalorie(calorie: Int) =
        mockMvc.perform(
            patch("/v1/users/me/daily-calorie")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"calorie": $calorie}"""),
        )
}
