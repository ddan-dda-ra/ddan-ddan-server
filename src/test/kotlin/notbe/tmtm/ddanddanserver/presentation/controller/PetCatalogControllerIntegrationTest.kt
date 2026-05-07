package notbe.tmtm.ddanddanserver.presentation.controller

import io.mockk.every
import io.mockk.mockk
import notbe.tmtm.ddanddanserver.application.service.PetCatalogService
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalog
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogBackgrounds
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogItem
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogLevel
import notbe.tmtm.ddanddanserver.presentation.filter.PetCatalogVersionFilter
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

/**
 * 통합 테스트 — Spring 컨텍스트에서 Controller + Filter wiring을 검증.
 *
 * Filter가 모든 응답에 X-Pet-Catalog-Version 헤더를 글로벌하게 추가하는지,
 * 그리고 Controller JSON 응답이 정상인지 동시에 확인.
 */
@WebMvcTest(
    controllers = [PetCatalogController::class],
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@Import(PetCatalogControllerIntegrationTest.TestConfig::class)
class PetCatalogControllerIntegrationTest {
    @TestConfiguration
    class TestConfig {
        @Bean
        fun petCatalogService(): PetCatalogService = mockk(relaxed = true)

        @Bean
        fun petCatalogVersionFilter(petCatalogService: PetCatalogService): PetCatalogVersionFilter =
            PetCatalogVersionFilter(petCatalogService)
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var petCatalogService: PetCatalogService

    @Test
    fun `GET catalog는 활성 펫 목록과 version을 반환하고 응답 헤더에 X-Pet-Catalog-Version을 포함한다`() {
        val now = Instant.parse("2026-05-04T12:00:00Z")
        every { petCatalogService.getActiveCatalog() } returns
            PetCatalog(
                version = now,
                pets =
                    listOf(
                        PetCatalogItem(
                            type = "CAT",
                            name = "고양이",
                            backgrounds =
                                PetCatalogBackgrounds(
                                    homeUrl = "https://cdn/cat/backgrounds/home.png",
                                    homeCompactUrl = "https://cdn/cat/backgrounds/home_compact.png",
                                    friendCardUrl = "https://cdn/cat/backgrounds/friend_card.png",
                                ),
                            isActive = true,
                            displayOrder = 0,
                            levels =
                                mapOf(
                                    1 to
                                        PetCatalogLevel(
                                            imageUrl = "https://cdn/cat_level1.png",
                                            lottieDefaultUrl = "https://cdn/cat_level1_default.json",
                                            lottiePlayEatUrl = "https://cdn/cat_level1_play_eat.json",
                                        ),
                                ),
                        ),
                    ),
            )
        every { petCatalogService.currentVersion() } returns now

        mockMvc
            .perform(get("/v1/pets/catalog"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.version").value("2026-05-04T12:00:00Z"))
            .andExpect(jsonPath("$.pets[0].type").value("CAT"))
            .andExpect(jsonPath("$.pets[0].name").value("고양이"))
            .andExpect(jsonPath("$.pets[0].levels.1.imageUrl").value("https://cdn/cat_level1.png"))
            .andExpect(header().string("X-Pet-Catalog-Version", "2026-05-04T12:00:00Z"))
    }
}
