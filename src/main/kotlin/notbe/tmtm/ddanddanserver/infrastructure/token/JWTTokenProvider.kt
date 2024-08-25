package notbe.tmtm.ddanddanserver.infrastructure.token

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwe
import io.jsonwebtoken.Jwts
import notbe.tmtm.ddanddanserver.domain.exception.AuthenticationExpiredAccessTokenException
import notbe.tmtm.ddanddanserver.domain.exception.AuthenticationExpiredRefreshTokenException
import notbe.tmtm.ddanddanserver.domain.exception.AuthenticationInvalidTokenException
import notbe.tmtm.ddanddanserver.domain.model.User
import notbe.tmtm.ddanddanserver.infrastructure.util.logger
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
    companion object {
        private const val TOKEN_TYPE = "token_type"
        private const val ACCESS = "access"
        private const val REFRESH = "refresh"
    }

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
            .add(TOKEN_TYPE, ACCESS)
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
            .add(TOKEN_TYPE, REFRESH)
            .and()
            .claims()
            .add("user_id", user.id)
            .and()
            .expiration(Date(System.currentTimeMillis() + refreshTokenExpiration * 1000))
            .encryptWith(secretKey, Jwts.ENC.A128CBC_HS256)
            .compact()

    fun validateRefreshToken(refreshToken: String) {
        val claims = getClaims(refreshToken, REFRESH)

        if (getTokenType(claims) != REFRESH) {
            throw AuthenticationInvalidTokenException()
        }
    }

    fun getUserIdFromRefreshToken(refreshToken: String): String {
        val claims = getClaims(refreshToken, REFRESH)

        if (getTokenType(claims) != ACCESS) {
            throw AuthenticationInvalidTokenException()
        }

        return getUserId(claims)
    }

    fun parseToken(accessToken: String): Authentication {
        val claims = getClaims(accessToken, ACCESS)

        if (getTokenType(claims) != ACCESS) {
            throw AuthenticationInvalidTokenException()
        }

        return UsernamePasswordAuthenticationToken(getUserId(claims), null, emptyList())
    }

    private fun getClaims(
        token: String,
        tokenType: String,
    ) = runCatching { jwtParser.parseEncryptedClaims(token) }
        .getOrElse {
            when (it) {
                is ExpiredJwtException ->
                    when (tokenType) {
                        ACCESS -> throw AuthenticationExpiredAccessTokenException()
                        REFRESH -> throw AuthenticationExpiredRefreshTokenException()
                        else -> throw AuthenticationInvalidTokenException()
                    }

                else -> throw AuthenticationInvalidTokenException()
            }
        }

    private fun getTokenType(claims: Jwe<Claims>): String =
        runCatching { claims.header[TOKEN_TYPE] as? String? ?: throw AuthenticationInvalidTokenException() }
            .getOrElse { throw AuthenticationInvalidTokenException() }

    private fun getUserId(claims: Jwe<Claims>): String =
        runCatching { claims.payload["user_id"] as? String ?: throw AuthenticationInvalidTokenException() }
            .getOrElse { throw AuthenticationInvalidTokenException() }
}
