package notbe.tmtm.ddanddanserver.presentation.filter

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import notbe.tmtm.ddanddanserver.application.service.PetCatalogService
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.time.Instant

class PetCatalogVersionFilterTest : FunSpec({
    test("응답에 X-Pet-Catalog-Version 헤더를 추가한다") {
        val service = mockk<PetCatalogService>()
        val filter = PetCatalogVersionFilter(service)
        every { service.currentVersion() } returns Instant.parse("2026-05-04T12:34:56Z")

        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        filter.doFilter(request, response, MockFilterChain())

        response.getHeader("X-Pet-Catalog-Version") shouldBe "2026-05-04T12:34:56Z"
    }

    test("catalog가 비어있을 때도 EPOCH 값을 헤더로 추가한다 (헤더 항상 존재)") {
        val service = mockk<PetCatalogService>()
        val filter = PetCatalogVersionFilter(service)
        every { service.currentVersion() } returns Instant.EPOCH

        val response = MockHttpServletResponse()
        filter.doFilter(MockHttpServletRequest(), response, MockFilterChain())

        response.getHeader("X-Pet-Catalog-Version") shouldBe Instant.EPOCH.toString()
    }
})
