package notbe.tmtm.ddanddanserver.application.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import notbe.tmtm.ddanddanserver.common.JWTTokenProvider
import notbe.tmtm.ddanddanserver.domain.exception.AdminInvalidCredentialsException

class AdminAuthServiceTest : FunSpec({
    lateinit var jwtTokenProvider: JWTTokenProvider
    lateinit var service: AdminAuthService

    beforeEach {
        jwtTokenProvider = mockk()
        service = AdminAuthService(
            configuredUsername = "ddan-ddan",
            configuredPassword = "admin0505",
            jwtTokenProvider = jwtTokenProvider,
        )
    }

    test("올바른 자격 증명이면 admin access token을 발급한다") {
        every { jwtTokenProvider.createAdminAccessToken("ddan-ddan") } returns "admin-token"

        val token = service.login("ddan-ddan", "admin0505")

        token shouldBe "admin-token"
        verify(exactly = 1) { jwtTokenProvider.createAdminAccessToken("ddan-ddan") }
    }

    test("username이 다르면 AdminInvalidCredentialsException") {
        shouldThrow<AdminInvalidCredentialsException> {
            service.login("intruder", "admin0505")
        }
    }

    test("password가 다르면 AdminInvalidCredentialsException") {
        shouldThrow<AdminInvalidCredentialsException> {
            service.login("ddan-ddan", "wrong-password")
        }
    }

    test("username/password 둘 다 다르면 AdminInvalidCredentialsException") {
        shouldThrow<AdminInvalidCredentialsException> {
            service.login("intruder", "wrong")
        }
    }

    test("자격 증명 실패 시 토큰 발급 메서드는 호출되지 않는다") {
        every { jwtTokenProvider.createAdminAccessToken(any()) } returns "should-not-be-called"

        runCatching { service.login("intruder", "admin0505") }

        verify(exactly = 0) { jwtTokenProvider.createAdminAccessToken(any()) }
    }

    test("길이가 다른 password도 안전하게 거부한다 (constant-time 비교)") {
        // 단순 equals라면 길이 다름을 즉시 거부 가능, 우리 구현은 MessageDigest.isEqual 사용
        shouldThrow<AdminInvalidCredentialsException> {
            service.login("ddan-ddan", "")
        }
        shouldThrow<AdminInvalidCredentialsException> {
            service.login("ddan-ddan", "admin0505x")
        }
    }

    test("올바른 자격 증명이면 발급된 토큰은 비어있지 않다") {
        every { jwtTokenProvider.createAdminAccessToken(any()) } returns "non-empty-token"

        val token = service.login("ddan-ddan", "admin0505")

        token shouldNotBe ""
    }
})
