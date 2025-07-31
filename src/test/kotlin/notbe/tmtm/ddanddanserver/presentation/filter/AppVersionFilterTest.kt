package notbe.tmtm.ddanddanserver.presentation.filter

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import notbe.tmtm.ddanddanserver.domain.exception.AppVersionUpgradeRequiredException
import org.springframework.web.servlet.HandlerExceptionResolver

class AppVersionFilterTest : BehaviorSpec({

    // Test wrapper to access protected method
    class TestableAppVersionFilter(
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

    given("헤더가 없는 요청") {
        `when`("필터가 실행되면") {
            val handlerExceptionResolver = mockk<HandlerExceptionResolver>(relaxed = true)
            val appVersionFilter = TestableAppVersionFilter(handlerExceptionResolver)
            val request = mockk<HttpServletRequest>(relaxed = true)
            val response = mockk<HttpServletResponse>(relaxed = true)
            val filterChain = mockk<FilterChain>(relaxed = true)

            every { request.requestURI } returns "/v1/users"
            every { request.getHeader("X-Android-Version") } returns null
            every { request.getHeader("X-iOS-Version") } returns null

            appVersionFilter.doFilterInternal(request, response, filterChain)

            then("필터를 통과해야 한다") {
                verify { filterChain.doFilter(request, response) }
            }
        }
    }

    given("제외 경로로 요청") {
        `when`("actuator 경로로 요청하면") {
            val handlerExceptionResolver = mockk<HandlerExceptionResolver>(relaxed = true)
            val appVersionFilter = TestableAppVersionFilter(handlerExceptionResolver)
            val request = mockk<HttpServletRequest>(relaxed = true)
            val response = mockk<HttpServletResponse>(relaxed = true)
            val filterChain = mockk<FilterChain>(relaxed = true)

            every { request.requestURI } returns "/actuator/health"

            appVersionFilter.doFilterInternal(request, response, filterChain)

            then("헤더 검증을 건너뛰고 통과해야 한다") {
                verify { filterChain.doFilter(request, response) }
                verify(exactly = 0) { request.getHeader(any()) }
            }
        }

        `when`("swagger-ui 경로로 요청하면") {
            val handlerExceptionResolver = mockk<HandlerExceptionResolver>(relaxed = true)
            val appVersionFilter = TestableAppVersionFilter(handlerExceptionResolver)
            val request = mockk<HttpServletRequest>(relaxed = true)
            val response = mockk<HttpServletResponse>(relaxed = true)
            val filterChain = mockk<FilterChain>(relaxed = true)

            every { request.requestURI } returns "/swagger-ui/index.html"

            appVersionFilter.doFilterInternal(request, response, filterChain)

            then("헤더 검증을 건너뛰고 통과해야 한다") {
                verify { filterChain.doFilter(request, response) }
                verify(exactly = 0) { request.getHeader(any()) }
            }
        }
    }

    given("유효한 버전 형식의 헤더") {
        `when`("유효한 Android 버전 형식으로 요청하면") {
            val handlerExceptionResolver = mockk<HandlerExceptionResolver>(relaxed = true)
            val appVersionFilter = TestableAppVersionFilter(handlerExceptionResolver)
            val request = mockk<HttpServletRequest>(relaxed = true)
            val response = mockk<HttpServletResponse>(relaxed = true)
            val filterChain = mockk<FilterChain>(relaxed = true)

            every { request.requestURI } returns "/v1/users"
            every { request.getHeader("X-Android-Version") } returns "1.2.3"
            every { request.getHeader("X-iOS-Version") } returns null

            appVersionFilter.doFilterInternal(request, response, filterChain)

            then("필터를 통과해야 한다") {
                verify { filterChain.doFilter(request, response) }
            }
        }

        `when`("유효한 iOS 버전 형식으로 요청하면") {
            val handlerExceptionResolver = mockk<HandlerExceptionResolver>(relaxed = true)
            val appVersionFilter = TestableAppVersionFilter(handlerExceptionResolver)
            val request = mockk<HttpServletRequest>(relaxed = true)
            val response = mockk<HttpServletResponse>(relaxed = true)
            val filterChain = mockk<FilterChain>(relaxed = true)

            every { request.requestURI } returns "/v1/users"
            every { request.getHeader("X-Android-Version") } returns null
            every { request.getHeader("X-iOS-Version") } returns "2.1.0"

            appVersionFilter.doFilterInternal(request, response, filterChain)

            then("필터를 통과해야 한다") {
                verify { filterChain.doFilter(request, response) }
            }
        }
    }

    given("잘못된 버전 형식의 헤더") {
        `when`("잘못된 Android 버전 형식으로 요청하면") {
            val handlerExceptionResolver = mockk<HandlerExceptionResolver>(relaxed = true)
            val appVersionFilter = TestableAppVersionFilter(handlerExceptionResolver)
            val request = mockk<HttpServletRequest>(relaxed = true)
            val response = mockk<HttpServletResponse>(relaxed = true)
            val filterChain = mockk<FilterChain>(relaxed = true)

            every { request.requestURI } returns "/v1/users"
            every { request.getHeader("X-Android-Version") } returns "1.2"
            every { request.getHeader("X-iOS-Version") } returns null

            appVersionFilter.doFilterInternal(request, response, filterChain)

            then("예외가 발생하고 요청이 차단되어야 한다") {
                verify {
                    handlerExceptionResolver.resolveException(
                        request,
                        response,
                        null,
                        any<AppVersionUpgradeRequiredException>()
                    )
                }
                verify(exactly = 0) { filterChain.doFilter(request, response) }
            }
        }

        `when`("잘못된 iOS 버전 형식으로 요청하면") {
            val handlerExceptionResolver = mockk<HandlerExceptionResolver>(relaxed = true)
            val appVersionFilter = TestableAppVersionFilter(handlerExceptionResolver)
            val request = mockk<HttpServletRequest>(relaxed = true)
            val response = mockk<HttpServletResponse>(relaxed = true)
            val filterChain = mockk<FilterChain>(relaxed = true)

            every { request.requestURI } returns "/v1/users"
            every { request.getHeader("X-Android-Version") } returns null
            every { request.getHeader("X-iOS-Version") } returns "invalid-version"

            appVersionFilter.doFilterInternal(request, response, filterChain)

            then("예외가 발생하고 요청이 차단되어야 한다") {
                verify {
                    handlerExceptionResolver.resolveException(
                        request,
                        response,
                        null,
                        any<AppVersionUpgradeRequiredException>()
                    )
                }
                verify(exactly = 0) { filterChain.doFilter(request, response) }
            }
        }
    }

    given("특수한 헤더 값") {
        `when`("빈 문자열 헤더로 요청하면") {
            val handlerExceptionResolver = mockk<HandlerExceptionResolver>(relaxed = true)
            val appVersionFilter = TestableAppVersionFilter(handlerExceptionResolver)
            val request = mockk<HttpServletRequest>(relaxed = true)
            val response = mockk<HttpServletResponse>(relaxed = true)
            val filterChain = mockk<FilterChain>(relaxed = true)

            every { request.requestURI } returns "/v1/users"
            every { request.getHeader("X-Android-Version") } returns ""
            every { request.getHeader("X-iOS-Version") } returns ""

            appVersionFilter.doFilterInternal(request, response, filterChain)

            then("헤더가 없는 것과 동일하게 처리되어 통과해야 한다") {
                verify { filterChain.doFilter(request, response) }
            }
        }

        `when`("공백 문자열 헤더로 요청하면") {
            val handlerExceptionResolver = mockk<HandlerExceptionResolver>(relaxed = true)
            val appVersionFilter = TestableAppVersionFilter(handlerExceptionResolver)
            val request = mockk<HttpServletRequest>(relaxed = true)
            val response = mockk<HttpServletResponse>(relaxed = true)
            val filterChain = mockk<FilterChain>(relaxed = true)

            every { request.requestURI } returns "/v1/users"
            every { request.getHeader("X-Android-Version") } returns "   "
            every { request.getHeader("X-iOS-Version") } returns null

            appVersionFilter.doFilterInternal(request, response, filterChain)

            then("헤더가 없는 것과 동일하게 처리되어 통과해야 한다") {
                verify { filterChain.doFilter(request, response) }
            }
        }
    }
})
