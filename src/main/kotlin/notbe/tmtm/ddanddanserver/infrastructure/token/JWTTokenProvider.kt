package notbe.tmtm.ddanddanserver.infrastructure.token

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwe
import io.jsonwebtoken.Jwts
import notbe.tmtm.ddanddanserver.domain.exception.AuthenticationExpiredAccessTokenException
import notbe.tmtm.ddanddanserver.domain.exception.AuthenticationExpiredRefreshTokenException
import notbe.tmtm.ddanddanserver.domain.exception.AuthenticationInvalidTokenException
import notbe.tmtm.ddanddanserver.domain.model.User
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Component
class JWTTokenProvider(
    @Value("\${app.jwt.secret}") private val secret: String,
    @Value("\${app.jwt.expiration.access-token}") private val accessTokenExpiration: Long,
    @Value("\${app.jwt.expiration.refresh-token}") private val refreshTokenExpiration: Long,
) {
    private final val secretKey: SecretKey = SecretKeySpec(secret.toByteArray(), "AES")

    private val jwtParser =
        Jwts
            .parser()
            .decryptWith(secretKey)
            .build()

    fun createAccessToken(user: User): String =
        Jwts
            .builder()
            .header()
            .add("token_type", "access")
            .and()
            .claims()
            .add("user_id", user.id)
            .and()
            .expiration(Date(System.currentTimeMillis() + accessTokenExpiration * 1000))
            .encryptWith(secretKey, Jwts.ENC.A128CBC_HS256)
            .compact()

    fun createRefreshToken(user: User): String =
        Jwts
            .builder()
            .header()
            .add("token_type", "refresh")
            .and()
            .claims()
            .add("user_id", user.id)
            .and()
            .expiration(Date(System.currentTimeMillis() + refreshTokenExpiration * 1000))
            .encryptWith(secretKey, Jwts.ENC.A128CBC_HS256)
            .compact()

    fun validateRefreshToken(refreshToken: String) {
        val claims = getClaims(refreshToken, "refresh")

        if (getTokenType(claims) != "refresh") {
            throw AuthenticationInvalidTokenException()
        }
    }

    fun getUserIdFromRefreshToken(refreshToken: String): String {
        val claims = getClaims(refreshToken, "refresh")

        if (getTokenType(claims) != "access") {
            throw AuthenticationInvalidTokenException()
        }

        return getUserId(claims)
    }

    fun parseToken(accessToken: String): Authentication {
        val claims = getClaims(accessToken, "access")

        if (getTokenType(claims) != "access") {
            throw AuthenticationInvalidTokenException()
        }

        return UsernamePasswordAuthenticationToken(getUserId(claims), null)
    }

    private fun getClaims(
        token: String,
        tokenType: String,
    ) = runCatching { jwtParser.parseEncryptedClaims(token) }
        .getOrElse {
            when (it) {
                is ExpiredJwtException ->
                    when (tokenType) {
                        "access" -> throw AuthenticationExpiredAccessTokenException()
                        "refresh" -> throw AuthenticationExpiredRefreshTokenException()
                        else -> throw AuthenticationInvalidTokenException()
                    }

                else -> throw AuthenticationInvalidTokenException()
            }
        }

    private fun getTokenType(claims: Jwe<Claims>): String =
        runCatching { claims.header["token_type"] as? String? ?: throw AuthenticationInvalidTokenException() }
            .getOrElse { throw AuthenticationInvalidTokenException() }

    private fun getUserId(claims: Jwe<Claims>): String =
        runCatching { claims.payload["user_id"] as? String ?: throw AuthenticationInvalidTokenException() }
            .getOrElse { throw AuthenticationInvalidTokenException() }
}
