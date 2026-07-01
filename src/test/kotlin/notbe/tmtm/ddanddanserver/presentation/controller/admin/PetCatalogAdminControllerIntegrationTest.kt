package notbe.tmtm.ddanddanserver.presentation.controller.admin

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import notbe.tmtm.ddanddanserver.application.service.PetCatalogService
import notbe.tmtm.ddanddanserver.application.service.UpsertPetCatalogCommand
import notbe.tmtm.ddanddanserver.domain.exception.PetCatalogInvalidException
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
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [PetCatalogAdminController::class], excludeAutoConfiguration = [SecurityAutoConfiguration::class])
@Import(PetCatalogAdminControllerIntegrationTest.TestConfig::class)
class PetCatalogAdminControllerIntegrationTest {
    @TestConfiguration
    class TestConfig {
        @Bean fun petCatalogService(): PetCatalogService = mockk(relaxed = true)
        @Bean fun userAdminService(): notbe.tmtm.ddanddanserver.application.service.UserAdminService = mockk(relaxed = true)
    }
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var service: PetCatalogService
    @Autowired lateinit var mapper: ObjectMapper

    private fun levels() = (1..5).map { PetCatalogLevel(it, "https://cdn.test/$it.svg", "https://cdn.test/$it.json", "https://cdn.test/${it}_play.json") }
    private fun item(active: Boolean) = PetCatalogItem.create("CAT", "고양이", "#AABBCC", active, 0, levels())
    private fun payload(active: Boolean = false, levels: Any = levels().map { mapOf("level" to it.level, "imageUrl" to it.imageUrl, "lottieDefaultUrl" to it.lottieDefaultUrl, "lottiePlayEatUrl" to it.lottiePlayEatUrl) }) =
        mapOf("type" to "CAT", "name" to "고양이", "colorCode" to "#AABBCC", "isActive" to active, "displayOrder" to 0, "levels" to levels)

    @BeforeEach fun reset() = clearMocks(service)

    @Test
    fun `관리자 목록은 비활성 여부와 levels 배열을 반환하고 backgrounds는 반환하지 않는다`() {
        every { service.getAllForAdmin() } returns listOf(item(false))
        mockMvc.perform(get("/v1/admin/pet-catalog"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.pets[0].isActive").value(false))
            .andExpect(jsonPath("$.pets[0].levels").isArray)
            .andExpect(jsonPath("$.pets[0].levels[0].level").value(1))
            .andExpect(jsonPath("$.pets[0].backgrounds").doesNotExist())
    }

    @Test
    fun `관리자 생성은 levels 배열을 command로 전달한다`() {
        every { service.create(any()) } returns item(false)
        mockMvc.perform(post("/v1/admin/pet-catalog").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(payload())))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.levels[4].level").value(5))
        verify { service.create(match<UpsertPetCatalogCommand> { it.levels.map(PetCatalogLevel::level) == (1..5).toList() }) }
    }

    @Test
    fun `관리자 생성에서 levels object Map은 decoding 실패로 400을 반환한다`() {
        mockMvc.perform(post("/v1/admin/pet-catalog").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(payload(levels = mapOf("1" to emptyMap<String, String>())))))
            .andExpect(status().isBadRequest)
        verify(exactly = 0) { service.create(any()) }
    }

    @Test
    fun `중복 누락 레벨과 잘못된 URL은 PC004 400으로 반환한다`() {
        every { service.create(any()) } throws PetCatalogInvalidException("invalid")
        val invalidLevels = listOf(
            listOf(1, 2, 3, 4, 4), listOf(1, 2, 3, 4), listOf(1, 2, 3, 4, 5),
        )
        invalidLevels.forEachIndexed { index, values ->
            val bodyLevels = values.map { level -> mapOf("level" to level, "imageUrl" to if (index == 2) "https://cdn.test/$level.gif" else "https://cdn.test/$level.svg", "lottieDefaultUrl" to "https://cdn.test/$level.json", "lottiePlayEatUrl" to "https://cdn.test/${level}_play.json") }
            mockMvc.perform(post("/v1/admin/pet-catalog").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(payload(levels = bodyLevels))))
                .andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("PC004"))
        }
    }

    @Test
    fun `관리자 수정은 false에서 true 활성화를 command로 전달한다`() {
        every { service.update(any(), any()) } returns item(true)
        val body = payload(active = true).minus("type")
        mockMvc.perform(put("/v1/admin/pet-catalog/cat").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)))
            .andExpect(status().isOk).andExpect(jsonPath("$.isActive").value(true))
        verify { service.update("cat", match { it.isActive }) }
    }

    @Test
    fun `관리자 수정에서 true에서 false 비활성화는 PC004 400을 반환한다`() {
        every { service.update(any(), any()) } throws PetCatalogInvalidException("활성 카탈로그는 비활성화할 수 없습니다")
        val body = payload(active = false).minus("type")
        mockMvc.perform(put("/v1/admin/pet-catalog/CAT").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("PC004"))
    }

    @Test
    fun `관리자 DELETE endpoint는 제거되어 404를 반환한다`() {
        mockMvc.perform(delete("/v1/admin/pet-catalog/CAT")).andExpect(status().isNotFound)
    }
}
