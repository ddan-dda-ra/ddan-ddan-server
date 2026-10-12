package notbe.tmtm.ddanddanserver.presentation.filter

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import notbe.tmtm.ddanddanserver.domain.exception.AppVersionUpgradeRequiredException
import notbe.tmtm.ddanddanserver.domain.exception.ErrorCode
import org.springframework.web.servlet.HandlerExceptionResolver

class AppVersionFilterTest : BehaviorSpec({

    // protected 메서드 접근용 래퍼
    class TestableAppVersionFilter(
        handlerExceptionResolver: HandlerExceptionResolver,
        androidMinimumVersion: String = "1.5.0",
        iosMinimumVersion: String = "1.5.0",
    ) : AppVersionFilter(handlerExceptionResolver, androidMinimumVersion, iosMinimumVersion) {

        public override fun doFilterInternal(
            request: HttpServletRequest,
            response: HttpServletResponse,
            filterChain: FilterChain,
        ) {
            super.doFilterInternal(request, response, filterChain)
        }
    }

    class Fixture(
        uri: String = "/v1/users",
        androidVersion: String? = null,
        iosVersion: String? = null,
        androidMinimumVersion: String = "1.5.0",
        iosMinimumVersion: String = "1.5.0",
    ) {
        val handlerExceptionResolver = mockk<HandlerExceptionResolver>(relaxed = true)
        val filter = TestableAppVersionFilter(handlerExceptionResolver, androidMinimumVersion, iosMinimumVersion)
        val request = mockk<HttpServletRequest>(relaxed = true)
        val response = mockk<HttpServletResponse>(relaxed = true)
        val filterChain = mockk<FilterChain>(relaxed = true)
        val exception = slot<Exception>()

        init {
            every { request.requestURI } returns uri
            every { request.getHeader("X-Android-Version") } returns androidVersion
            every { request.getHeader("X-iOS-Version") } returns iosVersion
            every { handlerExceptionResolver.resolveException(request, response, null, capture(exception)) } returns null
        }

        fun run() = filter.doFilterInternal(request, response, filterChain)

        fun verifyPassed() {
            verify { filterChain.doFilter(request, response) }
            exception.isCaptured shouldBe false
        }

        fun verifyBlocked(platform: String, currentVersion: String, minimumVersion: String) {
            verify(exactly = 0) { filterChain.doFilter(request, response) }
            val captured = exception.captured as AppVersionUpgradeRequiredException
            captured.errorCode shouldBe ErrorCode.APP_VERSION_UPGRADE_REQUIRED
            captured.data shouldBe mapOf(
                "platform" to platform,
                "currentVersion" to currentVersion,
                "minimumVersion" to minimumVersion,
            )
        }
    }

    given("헤더가 없는 요청") {
        `when`("필터가 실행되면") {
            val fixture = Fixture()
            fixture.run()

            then("하위 호환을 위해 통과해야 한다") {
                fixture.verifyPassed()
            }
        }
    }

    given("제외 경로로 요청") {
        listOf("/actuator/health", "/swagger-ui/index.html", "/v3/api-docs").forEach { uri ->
            `when`("$uri 경로로 낮은 버전 헤더와 함께 요청하면") {
                val fixture = Fixture(uri = uri, iosVersion = "1.0.0")
                fixture.run()

                then("헤더 검증을 건너뛰고 통과해야 한다") {
                    verify { fixture.filterChain.doFilter(fixture.request, fixture.response) }
                    verify(exactly = 0) { fixture.request.getHeader(any()) }
                }
            }
        }
    }

    given("최소 버전 이상의 헤더") {
        listOf(
            Triple("Android 최소 버전과 같으면", "1.5.0", null),
            Triple("Android 최소 버전보다 높으면", "1.10.0", null),
            Triple("iOS 최소 버전과 같으면", null, "1.5.0"),
            Triple("iOS 메이저 버전이 높으면", null, "2.0.0"),
        ).forEach { (name, android, ios) ->
            `when`(name) {
                val fixture = Fixture(androidVersion = android, iosVersion = ios)
                fixture.run()

                then("필터를 통과해야 한다") {
                    fixture.verifyPassed()
                }
            }
        }
    }

    given("최소 버전 미만의 헤더") {
        `when`("iOS 1.3.2로 요청하면") {
            val fixture = Fixture(iosVersion = "1.3.2")
            fixture.run()

            then("426 예외로 차단되고 최소 버전을 알려줘야 한다") {
                fixture.verifyBlocked(platform = "ios", currentVersion = "1.3.2", minimumVersion = "1.5.0")
            }
        }

        `when`("Android 1.4.1로 요청하면") {
            val fixture = Fixture(androidVersion = "1.4.1")
            fixture.run()

            then("426 예외로 차단되고 최소 버전을 알려줘야 한다") {
                fixture.verifyBlocked(platform = "android", currentVersion = "1.4.1", minimumVersion = "1.5.0")
            }
        }

        `when`("패치 버전만 낮은 iOS 1.4.9로 요청하면") {
            val fixture = Fixture(iosVersion = "1.4.9")
            fixture.run()

            then("426 예외로 차단되어야 한다") {
                fixture.verifyBlocked(platform = "ios", currentVersion = "1.4.9", minimumVersion = "1.5.0")
            }
        }
    }

    given("플랫폼별 최소 버전이 다르게 설정된 경우") {
        `when`("iOS 최소 버전만 1.6.0이고 iOS 1.5.0으로 요청하면") {
            val fixture = Fixture(iosVersion = "1.5.0", androidMinimumVersion = "1.0.0", iosMinimumVersion = "1.6.0")
            fixture.run()

            then("iOS 설정값 기준으로 차단되어야 한다") {
                fixture.verifyBlocked(platform = "ios", currentVersion = "1.5.0", minimumVersion = "1.6.0")
            }
        }

        `when`("Android 최소 버전만 1.0.0이고 Android 1.4.1로 요청하면") {
            val fixture = Fixture(androidVersion = "1.4.1", androidMinimumVersion = "1.0.0", iosMinimumVersion = "1.6.0")
            fixture.run()

            then("Android 설정값 기준으로 통과해야 한다") {
                fixture.verifyPassed()
            }
        }
    }

    given("두 플랫폼 헤더가 함께 오는 경우") {
        `when`("Android 1.5.0과 iOS 1.0.0이 함께 오면") {
            val fixture = Fixture(androidVersion = "1.5.0", iosVersion = "1.0.0")
            fixture.run()

            then("Android 헤더만 검사해 통과해야 한다") {
                fixture.verifyPassed()
            }
        }

        `when`("Android 1.0.0과 iOS 2.0.0이 함께 오면") {
            val fixture = Fixture(androidVersion = "1.0.0", iosVersion = "2.0.0")
            fixture.run()

            then("Android 기준으로 차단되어야 한다") {
                fixture.verifyBlocked(platform = "android", currentVersion = "1.0.0", minimumVersion = "1.5.0")
            }
        }
    }

    given("잘못된 버전 형식의 헤더") {
        `when`("Android 버전이 두 자리 형식이면") {
            val fixture = Fixture(androidVersion = "1.5")
            fixture.run()

            then("형식 오류로 차단되어야 한다") {
                fixture.verifyBlocked(platform = "android", currentVersion = "1.5", minimumVersion = "1.5.0")
            }
        }

        `when`("iOS 버전이 문자열이면") {
            val fixture = Fixture(iosVersion = "invalid-version")
            fixture.run()

            then("형식 오류로 차단되어야 한다") {
                fixture.verifyBlocked(platform = "ios", currentVersion = "invalid-version", minimumVersion = "1.5.0")
            }
        }
    }

    given("특수한 헤더 값") {
        `when`("빈 문자열 헤더로 요청하면") {
            val fixture = Fixture(androidVersion = "", iosVersion = "")
            fixture.run()

            then("헤더가 없는 것과 동일하게 통과해야 한다") {
                fixture.verifyPassed()
            }
        }

        `when`("공백 문자열 헤더로 요청하면") {
            val fixture = Fixture(androidVersion = "   ")
            fixture.run()

            then("헤더가 없는 것과 동일하게 통과해야 한다") {
                fixture.verifyPassed()
            }
        }
    }

    given("최소 버전 설정값이 잘못된 경우") {
        `when`("필터를 생성하면") {
            then("기동 시점에 예외가 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    TestableAppVersionFilter(mockk(relaxed = true), androidMinimumVersion = "1.5")
                }
            }
        }
    }
})
