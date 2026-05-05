package notbe.tmtm.ddanddanserver.presentation.controller.admin

import io.mockk.every
import io.mockk.mockk
import notbe.tmtm.ddanddanserver.application.service.PetCatalogService
import notbe.tmtm.ddanddanserver.application.service.UserAdminService
import notbe.tmtm.ddanddanserver.domain.model.user.DeviceToken
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.domain.model.user.UserSetting
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(
    controllers = [UserAdminController::class],
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@Import(UserAdminControllerIntegrationTest.TestConfig::class)
class UserAdminControllerIntegrationTest {
    @TestConfiguration
    class TestConfig {
        @Bean
        fun userAdminService(): UserAdminService = mockk(relaxed = true)

        // PetCatalogVersionFilter가 글로벌이라 컨텍스트에 PetCatalogService도 필요
        @Bean
        fun petCatalogService(): PetCatalogService = mockk(relaxed = true)
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var userAdminService: UserAdminService

    private fun user(
        name: String?,
        tickets: Int = 0,
    ): User =
        User(
            id = ObjectId(),
            deviceToken = DeviceToken("token"),
            name = name,
            tickets = tickets,
            setting = UserSetting(),
        )

    @Test
    fun `GET v1 admin users는 페이지 응답을 반환한다`() {
        every { userAdminService.searchUsers(any(), any(), any(), any()) } returns
            PageImpl(listOf(user("ddingmin", tickets = 3), user("hardyoon")))

        mockMvc
            .perform(get("/v1/admin/users").param("page", "0").param("size", "20"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.users[0].name").value("ddingmin"))
            .andExpect(jsonPath("$.users[0].tickets").value(3))
            .andExpect(jsonPath("$.users[1].name").value("hardyoon"))
            .andExpect(jsonPath("$.page.totalElements").value(2))
    }

    @Test
    fun `GET v1 admin users id는 유저 상세를 반환한다`() {
        val id = ObjectId()
        every { userAdminService.getUser(id) } returns user("ddingmin", tickets = 5)

        mockMvc
            .perform(get("/v1/admin/users/${id.toHexString()}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("ddingmin"))
            .andExpect(jsonPath("$.tickets").value(5))
    }
}
