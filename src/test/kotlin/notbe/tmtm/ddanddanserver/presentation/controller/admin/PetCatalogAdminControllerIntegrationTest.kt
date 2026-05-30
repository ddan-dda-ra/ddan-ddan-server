package notbe.tmtm.ddanddanserver.presentation.controller.admin

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import notbe.tmtm.ddanddanserver.application.service.PetCatalogService
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogBackgrounds
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogItem
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogLevel
import notbe.tmtm.ddanddanserver.presentation.dto.admin.PetCatalogAdminCreateRequest
import notbe.tmtm.ddanddanserver.presentation.dto.admin.PetCatalogAdminUpdateRequest
import notbe.tmtm.ddanddanserver.presentation.dto.admin.PetCatalogBackgroundsRequest
import notbe.tmtm.ddanddanserver.presentation.dto.admin.PetCatalogLevelRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(
    controllers = [PetCatalogAdminController::class],
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@Import(PetCatalogAdminControllerIntegrationTest.TestConfig::class)
class PetCatalogAdminControllerIntegrationTest {
    @TestConfiguration
    class TestConfig {
        @Bean
        fun petCatalogService(): PetCatalogService = mockk(relaxed = true)

        // UserAdminController의 의존성. WebMvcTest가 같은 패키지의 다른 controller도 스캔하면 필요
        @Bean
        fun userAdminService(): notbe.tmtm.ddanddanserver.application.service.UserAdminService = mockk(relaxed = true)
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var petCatalogService: PetCatalogService

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun resetMocks() {
        clearMocks(petCatalogService)
    }

    private fun item(
        key: String,
        order: Int = 0,
        isActive: Boolean = true,
        colorCode: String = "#FFCC00",
    ): PetCatalogItem =
        PetCatalogItem(
            type = key,
            name = "이름-$key",
            backgrounds =
                PetCatalogBackgrounds(
                    homeUrl = "https://cdn/x/backgrounds/home.png",
                    homeCompactUrl = "https://cdn/x/backgrounds/home_compact.png",
                    friendCardUrl = "https://cdn/x/backgrounds/friend_card.png",
                ),
            colorCode = colorCode,
            isActive = isActive,
            displayOrder = order,
            levels =
                mapOf(
                    1 to
                        PetCatalogLevel(
                            imageUrl = "https://cdn/x/1.png",
                            lottieDefaultUrl = "https://cdn/x/1_default.json",
                            lottiePlayEatUrl = "https://cdn/x/1_play_eat.json",
                        ),
                ),
        )

    private fun sampleBackgroundsRequest(): PetCatalogBackgroundsRequest =
        PetCatalogBackgroundsRequest(
            homeUrl = "u/backgrounds/home.png",
            homeCompactUrl = "u/backgrounds/home_compact.png",
            friendCardUrl = "u/backgrounds/friend_card.png",
        )

    @Test
    fun `GET v1 admin pet-catalog는 비활성 포함 전체를 반환한다`() {
        every { petCatalogService.getAllForAdmin() } returns
            listOf(item("CAT", 0, true), item("HIDDEN", 1, false))

        mockMvc
            .perform(get("/v1/admin/pet-catalog"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.pets[0].type").value("CAT"))
            .andExpect(jsonPath("$.pets[0].isActive").value(true))
            .andExpect(jsonPath("$.pets[0].colorCode").value("#FFCC00"))
            .andExpect(jsonPath("$.pets[0].backgrounds.homeUrl").value("https://cdn/x/backgrounds/home.png"))
            .andExpect(jsonPath("$.pets[1].type").value("HIDDEN"))
            .andExpect(jsonPath("$.pets[1].isActive").value(false))
    }

    @Test
    fun `POST v1 admin pet-catalog는 신규 펫을 등록한다`() {
        every { petCatalogService.create(any(), any(), any(), any(), any(), any(), any()) } returns
            item("QUOKKA", 5, colorCode = "#FFCC00")
        val request =
            PetCatalogAdminCreateRequest(
                type = "QUOKKA",
                name = "쿼카",
                backgrounds = sampleBackgroundsRequest(),
                colorCode = "#FFCC00",
                isActive = true,
                displayOrder = 5,
                levels =
                    mapOf(
                        1 to PetCatalogLevelRequest("u/1.png", "u/1_default.json", "u/1_play_eat.json"),
                    ),
            )

        mockMvc
            .perform(
                post("/v1/admin/pet-catalog")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.type").value("QUOKKA"))
            .andExpect(jsonPath("$.colorCode").value("#FFCC00"))

        verify { petCatalogService.create("QUOKKA", "쿼카", any(), "#FFCC00", true, 5, any()) }
    }

    @Test
    fun `POST v1 admin pet-catalog는 colorCode가 hex 정규식에 어긋나면 400을 반환한다`() {
        val invalidColors = listOf("red", "#GGGGGG", "#FFF", "#FFCC0080", "rgba(255,0,0,1)")

        invalidColors.forEach { invalid ->
            val payload =
                mapOf(
                    "type" to "QUOKKA",
                    "name" to "쿼카",
                    "backgrounds" to
                        mapOf(
                            "homeUrl" to "u/backgrounds/home.png",
                            "homeCompactUrl" to "u/backgrounds/home_compact.png",
                            "friendCardUrl" to "u/backgrounds/friend_card.png",
                        ),
                    "colorCode" to invalid,
                    "isActive" to true,
                    "displayOrder" to 5,
                    "levels" to
                        mapOf(
                            "1" to
                                mapOf(
                                    "imageUrl" to "u/1.png",
                                    "lottieDefaultUrl" to "u/1_default.json",
                                    "lottiePlayEatUrl" to "u/1_play_eat.json",
                                ),
                        ),
                )

            mockMvc
                .perform(
                    post("/v1/admin/pet-catalog")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)),
                ).andExpect(status().isBadRequest)
        }

        verify(exactly = 0) {
            petCatalogService.create(any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `PUT v1 admin pet-catalog는 펫을 수정한다`() {
        every { petCatalogService.update(any(), any(), any(), any(), any(), any(), any()) } returns
            item("CAT", 99, false, colorCode = "#9B6CFF")
        val request =
            PetCatalogAdminUpdateRequest(
                name = "갱신",
                backgrounds = sampleBackgroundsRequest(),
                colorCode = "#9B6CFF",
                isActive = false,
                displayOrder = 99,
                levels = mapOf(1 to PetCatalogLevelRequest("a", "b", "c")),
            )

        mockMvc
            .perform(
                put("/v1/admin/pet-catalog/CAT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.isActive").value(false))
            .andExpect(jsonPath("$.displayOrder").value(99))
            .andExpect(jsonPath("$.colorCode").value("#9B6CFF"))

        verify { petCatalogService.update("CAT", "갱신", any(), "#9B6CFF", false, 99, any()) }
    }

    @Test
    fun `DELETE v1 admin pet-catalog는 soft delete를 수행한다`() {
        every { petCatalogService.softDelete("CAT") } returns item("CAT", 0, false)

        mockMvc
            .perform(delete("/v1/admin/pet-catalog/CAT"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isActive").value(false))

        verify { petCatalogService.softDelete("CAT") }
    }
}
