package notbe.tmtm.ddanddanserver.presentation.filter

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import notbe.tmtm.ddanddanserver.common.JWTTokenProvider
import notbe.tmtm.ddanddanserver.domain.exception.AdminInvalidTokenException
import notbe.tmtm.ddanddanserver.domain.exception.AdminUnauthorizedException
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.servlet.HandlerExceptionResolver

class AdminJWTAuthFilterTest : FunSpec({
    lateinit var jwtTokenProvider: JWTTokenProvider
    lateinit var resolver: HandlerExceptionResolver
    lateinit var filter: AdminJWTAuthFilter

    beforeEach {
        jwtTokenProvider = mockk()
        resolver = mockk(relaxed = true)
        filter = AdminJWTAuthFilter(jwtTokenProvider, resolver)
        SecurityContextHolder.clearContext()
    }

    test("Authorization 헤더가 없으면 AdminUnauthorizedException으로 흘려보낸다") {
        val request = MockHttpServletRequest("GET", "/v1/admin/users")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        verify { resolver.resolveException(request, response, null, any<AdminUnauthorizedException>()) }
    }

    test("Bearer prefix가 없는 헤더도 미인증 처리") {
        val request = MockHttpServletRequest("GET", "/v1/admin/users")
        request.addHeader("Authorization", "RawTokenWithoutBearer")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        verify { resolver.resolveException(request, response, null, any<AdminUnauthorizedException>()) }
    }

    test("정상 admin 토큰이면 SecurityContext에 인증 객체를 설정하고 chain을 진행한다") {
        val request = MockHttpServletRequest("GET", "/v1/admin/users")
        request.addHeader("Authorization", "Bearer valid-token")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        every { jwtTokenProvider.parseAdminToken("valid-token") } returns
            UsernamePasswordAuthenticationToken(
                "ddan-ddan",
                null,
                listOf(SimpleGrantedAuthority(JWTTokenProvider.ROLE_ADMIN)),
            )

        filter.doFilter(request, response, chain)

        SecurityContextHolder.getContext().authentication.principal shouldBe "ddan-ddan"
        chain.request shouldBe request
    }

    test("잘못된 토큰이면 SecurityContext가 비워지고 예외가 흘려보내진다") {
        val request = MockHttpServletRequest("GET", "/v1/admin/users")
        request.addHeader("Authorization", "Bearer bad-token")
        val response = MockHttpServletResponse()

        every { jwtTokenProvider.parseAdminToken("bad-token") } throws AdminInvalidTokenException()

        filter.doFilter(request, response, MockFilterChain())

        SecurityContextHolder.getContext().authentication shouldBe null
        verify { resolver.resolveException(request, response, null, any<AdminInvalidTokenException>()) }
    }
})
