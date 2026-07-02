package notbe.tmtm.ddanddanserver.presentation.controller

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import notbe.tmtm.ddanddanserver.application.service.PetCatalogService
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalog
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogItem
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogLevel
import org.junit.jupiter.api.BeforeEach
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@WebMvcTest(controllers = [PetCatalogController::class], excludeAutoConfiguration = [SecurityAutoConfiguration::class])
@Import(PetCatalogControllerIntegrationTest.TestConfig::class)
class PetCatalogControllerIntegrationTest {
    @TestConfiguration
    class TestConfig {
        @Bean fun petCatalogService(): PetCatalogService = mockk(relaxed = true)
    }

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var petCatalogService: PetCatalogService

    private fun catalog() = PetCatalog(
        revision = Instant.parse("2026-07-01T12:34:56Z"),
        pets = listOf(PetCatalogItem.create(
            "CAT", "고양이", "#AABBCC", true, 0,
            (1..5).reversed().map { PetCatalogLevel(it, "https://cdn.test/$it.svg", "https://cdn.test/$it.json", "https://cdn.test/${it}_play.json") },
        )),
    )

    @BeforeEach fun reset() {
        clearMocks(petCatalogService)
        every { petCatalogService.getCatalog() } returns catalog()
    }

    @Test
    fun `공개 응답은 항상 200으로 revision과 levels 배열을 반환하고 내부 필드를 노출하지 않는다`() {
        mockMvc.perform(get("/v1/pets/catalog"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.revision").value("2026-07-01T12:34:56Z"))
            .andExpect(jsonPath("$.version").doesNotExist())
            .andExpect(jsonPath("$.pets[0].isActive").doesNotExist())
            .andExpect(jsonPath("$.pets[0].backgrounds").doesNotExist())
            .andExpect(jsonPath("$.pets[0].colorCode").value("#AABBCC"))
            .andExpect(jsonPath("$.pets[0].levels").isArray)
            .andExpect(jsonPath("$.pets[0].levels[0].level").value(1))
            .andExpect(jsonPath("$.pets[0].levels[4].level").value(5))
    }

    @Test
    fun `활성 카탈로그가 비어 있으면 body revision은 Instant EPOCH ISO 문자열이다`() {
        every { petCatalogService.getCatalog() } returns PetCatalog(Instant.EPOCH, emptyList())

        mockMvc.perform(get("/v1/pets/catalog"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.revision").value("1970-01-01T00:00:00Z"))
            .andExpect(jsonPath("$.pets").isEmpty)
    }

    @Test
    fun `조건부 요청 헤더가 있어도 controller는 항상 200 body를 반환한다`() {
        mockMvc.perform(
            get("/v1/pets/catalog")
                .header("If-None-Match", "\"stale-catalog\"")
                .header("X-Pet-Catalog-Version", "old-revision"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.revision").value("2026-07-01T12:34:56Z"))
            .andExpect(jsonPath("$.pets").isArray)
            .andExpect(header().doesNotExist("ETag"))
            .andExpect(header().doesNotExist("Cache-Control"))
            .andExpect(header().doesNotExist("X-Pet-Catalog-Version"))
    }
}
