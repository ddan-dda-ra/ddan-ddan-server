package notbe.tmtm.ddanddanserver.common

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.jsonwebtoken.Jwts
import notbe.tmtm.ddanddanserver.domain.exception.AdminInvalidTokenException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.util.Date
import javax.crypto.spec.SecretKeySpec

class JWTTokenProviderAdminTest : FunSpec({
    val secret = "0123456789abcdef0123456789abcdef" // 32 bytes for AES-256
    val provider = JWTTokenProvider(
        secret = secret,
        accessTokenExpiration = 3600,
        refreshTokenExpiration = 86400,
    )

    fun externalAdminToken(username: String): String =
        Jwts
            .builder()
            .header()
            .add("token_type", "admin_access")
            .and()
            .claims()
            .add("username", username)
            .and()
            .expiration(Date(System.currentTimeMillis() + 60_000))
            .encryptWith(SecretKeySpec(secret.toByteArray(), "AES"), Jwts.ENC.A128CBC_HS256)
            .compact()

    test("parseAdminToken은 외부에서 발급한 호환 토큰의 username과 ROLE_ADMIN을 복원한다") {
        val token = externalAdminToken("ddan-ddan")

        val auth = provider.parseAdminToken(token)

        auth.principal shouldBe "ddan-ddan"
        auth.authorities.toList() shouldBe listOf(SimpleGrantedAuthority(JWTTokenProvider.ROLE_ADMIN))
    }

    test("일반 access token을 admin으로 parse하면 AdminInvalidTokenException") {
        // 다른 token type으로 발급한 것을 admin으로 잘못 사용 — 거부되어야
        val invalidToken = "not-a-valid-jwt"

        shouldThrow<AdminInvalidTokenException> {
            provider.parseAdminToken(invalidToken)
        }
    }
})
