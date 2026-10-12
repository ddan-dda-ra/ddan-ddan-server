package notbe.tmtm.ddanddanserver.common

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import notbe.tmtm.ddanddanserver.domain.model.user.User

class JWTTokenProviderTest : FunSpec({

    val secret = "0123456789abcdef0123456789abcdef"
    val provider = JWTTokenProvider(secret, accessTokenExpiration = 3600, refreshTokenExpiration = 3600)
    val expiredProvider = JWTTokenProvider(secret, accessTokenExpiration = -60, refreshTokenExpiration = -60)
    val user = User.register(deviceToken = null)

    context("extractUserIdOrNull") {
        test("유효한 액세스 토큰이면 유저 ID를 반환한다") {
            provider.extractUserIdOrNull(provider.createAccessToken(user)) shouldBe user.id
        }

        test("만료된 액세스 토큰이어도 유저 ID를 반환한다") {
            provider.extractUserIdOrNull(expiredProvider.createAccessToken(user)) shouldBe user.id
        }

        test("다른 키로 만든 토큰이면 null을 반환한다") {
            val otherProvider = JWTTokenProvider("fedcba9876543210fedcba9876543210", 3600, 3600)
            provider.extractUserIdOrNull(otherProvider.createAccessToken(user)).shouldBeNull()
        }

        test("토큰 형식이 아니면 예외 없이 null을 반환한다") {
            provider.extractUserIdOrNull("not-a-token").shouldBeNull()
            provider.extractUserIdOrNull("").shouldBeNull()
        }
    }
})
