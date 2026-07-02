package notbe.tmtm.ddanddanserver.presentation.filter

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import notbe.tmtm.ddanddanserver.application.service.PetCatalogService
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.time.Instant

class PetCatalogRevisionFilterTest : FunSpec({
    lateinit var service: PetCatalogService
    lateinit var filter: PetCatalogRevisionFilter
    val revision = Instant.parse("2026-07-02T00:00:00Z")

    beforeEach {
        service = mockk()
        filter = PetCatalogRevisionFilter(service)
    }

    test("클라이언트 revision이 현재 값과 같으면 다운로드가 필요하지 않다") {
        every { service.currentRevision() } returns revision
        val request = MockHttpServletRequest("GET", "/v1/pets/me")
        request.addHeader(PetCatalogRevisionFilter.REQUEST_REVISION_HEADER, revision.toString())
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        response.getHeader(PetCatalogRevisionFilter.DOWNLOAD_REQUIRED_HEADER) shouldBe "false"
    }

    test("클라이언트 revision이 다르면 다운로드가 필요하다") {
        every { service.currentRevision() } returns revision
        val request = MockHttpServletRequest("GET", "/v1/pets/me")
        request.addHeader(PetCatalogRevisionFilter.REQUEST_REVISION_HEADER, "2026-07-01T00:00:00Z")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        response.getHeader(PetCatalogRevisionFilter.DOWNLOAD_REQUIRED_HEADER) shouldBe "true"
    }

    test("revision 헤더가 없거나 형식이 잘못되면 다운로드가 필요하다") {
        every { service.currentRevision() } returns revision
        listOf(null, "invalid-revision").forEach { clientRevision ->
            val request = MockHttpServletRequest("GET", "/v1/pets/me")
            clientRevision?.let { request.addHeader(PetCatalogRevisionFilter.REQUEST_REVISION_HEADER, it) }
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, MockFilterChain())

            response.getHeader(PetCatalogRevisionFilter.DOWNLOAD_REQUIRED_HEADER) shouldBe "true"
        }
    }

    test("로그인 API는 revision을 조회하거나 다운로드 필요 헤더를 추가하지 않는다") {
        val request = MockHttpServletRequest("POST", "/v1/auth/login")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        response.getHeader(PetCatalogRevisionFilter.DOWNLOAD_REQUIRED_HEADER) shouldBe null
        verify(exactly = 0) { service.currentRevision() }
    }

    test("토큰 재발급 API는 revision 비교 대상이다") {
        every { service.currentRevision() } returns revision
        val request = MockHttpServletRequest("POST", "/v1/auth/reissue")
        request.addHeader(PetCatalogRevisionFilter.REQUEST_REVISION_HEADER, revision.toString())
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        response.getHeader(PetCatalogRevisionFilter.DOWNLOAD_REQUIRED_HEADER) shouldBe "false"
    }

    test("revision 조회에 실패해도 요청은 계속하고 다운로드 필요로 응답한다") {
        every { service.currentRevision() } throws IllegalStateException("database unavailable")
        val request = MockHttpServletRequest("GET", "/v1/pets/me")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        response.getHeader(PetCatalogRevisionFilter.DOWNLOAD_REQUIRED_HEADER) shouldBe "true"
        chain.request shouldBe request
    }
})
