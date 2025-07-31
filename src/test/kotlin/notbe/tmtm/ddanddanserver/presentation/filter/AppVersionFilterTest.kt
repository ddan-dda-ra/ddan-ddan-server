package notbe.tmtm.ddanddanserver.presentation.filter

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import notbe.tmtm.ddanddanserver.domain.exception.AppVersionUpgradeRequiredException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.servlet.HandlerExceptionResolver

class AppVersionFilterTest {
    private lateinit var appVersionFilter: TestableAppVersionFilter
    private lateinit var handlerExceptionResolver: HandlerExceptionResolver
    private lateinit var request: HttpServletRequest
    private lateinit var response: HttpServletResponse
    private lateinit var filterChain: FilterChain

    // Test wrapper to access protected method
    private class TestableAppVersionFilter(
        handlerExceptionResolver: HandlerExceptionResolver
    ) : AppVersionFilter(handlerExceptionResolver) {
        
        public override fun doFilterInternal(
            request: HttpServletRequest,
            response: HttpServletResponse,
            filterChain: FilterChain
        ) {
            super.doFilterInternal(request, response, filterChain)
        }
    }

    @BeforeEach
    fun setUp() {
        handlerExceptionResolver = mockk(relaxed = true)
        appVersionFilter = TestableAppVersionFilter(handlerExceptionResolver)
        request = mockk(relaxed = true)
        response = mockk(relaxed = true)
        filterChain = mockk(relaxed = true)
    }

    @Test
    fun `헤더가 없으면 필터를 통과해야 한다`() {
        // given
        every { request.requestURI } returns "/v1/users"
        every { request.getHeader("X-Android-Version") } returns null
        every { request.getHeader("X-iOS-Version") } returns null

        // when
        appVersionFilter.doFilterInternal(request, response, filterChain)

        // then
        verify { filterChain.doFilter(request, response) }
    }

    @Test
    fun `excluded path는 검증을 건너뛰어야 한다`() {
        // given
        every { request.requestURI } returns "/actuator/health"

        // when
        appVersionFilter.doFilterInternal(request, response, filterChain)

        // then
        verify { filterChain.doFilter(request, response) }
        verify(exactly = 0) { request.getHeader(any()) }
    }

    @Test
    fun `swagger-ui path는 검증을 건너뛰어야 한다`() {
        // given
        every { request.requestURI } returns "/swagger-ui/index.html"

        // when
        appVersionFilter.doFilterInternal(request, response, filterChain)

        // then
        verify { filterChain.doFilter(request, response) }
        verify(exactly = 0) { request.getHeader(any()) }
    }

    @Test
    fun `유효한 Android 버전 형식이면 통과해야 한다`() {
        // given
        every { request.requestURI } returns "/v1/users"
        every { request.getHeader("X-Android-Version") } returns "1.2.3"
        every { request.getHeader("X-iOS-Version") } returns null

        // when
        appVersionFilter.doFilterInternal(request, response, filterChain)

        // then
        verify { filterChain.doFilter(request, response) }
    }

    @Test
    fun `유효한 iOS 버전 형식이면 통과해야 한다`() {
        // given
        every { request.requestURI } returns "/v1/users"
        every { request.getHeader("X-Android-Version") } returns null
        every { request.getHeader("X-iOS-Version") } returns "2.1.0"

        // when
        appVersionFilter.doFilterInternal(request, response, filterChain)

        // then
        verify { filterChain.doFilter(request, response) }
    }

    @Test
    fun `잘못된 Android 버전 형식이면 예외가 발생해야 한다`() {
        // given
        every { request.requestURI } returns "/v1/users"
        every { request.getHeader("X-Android-Version") } returns "1.2"
        every { request.getHeader("X-iOS-Version") } returns null

        // when
        appVersionFilter.doFilterInternal(request, response, filterChain)

        // then
        verify { handlerExceptionResolver.resolveException(request, response, null, any<AppVersionUpgradeRequiredException>()) }
        verify(exactly = 0) { filterChain.doFilter(request, response) }
    }

    @Test
    fun `잘못된 iOS 버전 형식이면 예외가 발생해야 한다`() {
        // given
        every { request.requestURI } returns "/v1/users"
        every { request.getHeader("X-Android-Version") } returns null
        every { request.getHeader("X-iOS-Version") } returns "invalid-version"

        // when
        appVersionFilter.doFilterInternal(request, response, filterChain)

        // then
        verify { handlerExceptionResolver.resolveException(request, response, null, any<AppVersionUpgradeRequiredException>()) }
        verify(exactly = 0) { filterChain.doFilter(request, response) }
    }

    @Test
    fun `빈 문자열 헤더는 헤더가 없는 것과 동일하게 처리되어야 한다`() {
        // given
        every { request.requestURI } returns "/v1/users"
        every { request.getHeader("X-Android-Version") } returns ""
        every { request.getHeader("X-iOS-Version") } returns ""

        // when
        appVersionFilter.doFilterInternal(request, response, filterChain)

        // then
        verify { filterChain.doFilter(request, response) }
    }

    @Test
    fun `공백 문자열 헤더는 헤더가 없는 것과 동일하게 처리되어야 한다`() {
        // given
        every { request.requestURI } returns "/v1/users"
        every { request.getHeader("X-Android-Version") } returns "   "
        every { request.getHeader("X-iOS-Version") } returns null

        // when
        appVersionFilter.doFilterInternal(request, response, filterChain)

        // then
        verify { filterChain.doFilter(request, response) }
    }
}
