package notbe.tmtm.ddanddanserver.common

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwe
import io.jsonwebtoken.Jwts
import notbe.tmtm.ddanddanserver.common.util.logger
import notbe.tmtm.ddanddanserver.domain.exception.AdminExpiredTokenException
import notbe.tmtm.ddanddanserver.domain.exception.AdminInvalidTokenException
import notbe.tmtm.ddanddanserver.domain.exception.AuthenticationExpiredAccessTokenException
import notbe.tmtm.ddanddanserver.domain.exception.AuthenticationExpiredRefreshTokenException
import notbe.tmtm.ddanddanserver.domain.exception.AuthenticationInvalidTokenException
import notbe.tmtm.ddanddanserver.domain.model.user.User
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Component
class JWTTokenProvider(
    @Value("\${app.jwt.secret}") private val secret: String,
    @Value("\${app.jwt.expiration.access-token}") private val accessTokenExpiration: Long,
    @Value("\${app.jwt.expiration.refresh-token}") private val refreshTokenExpiration: Long,
) {
    val logger = logger()

    companion object {
        private const val TOKEN_TYPE = "token_type"
        private const val ACCESS = "access"
        private const val REFRESH = "refresh"
        private const val ADMIN_ACCESS = "admin_access"
        const val ROLE_ADMIN = "ROLE_ADMIN"
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
            .add("user_id", user.id.toHexString())
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
            .add("user_id", user.id.toHexString())
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

    fun getUserIdFromRefreshToken(refreshToken: String): ObjectId {
        val claims = getClaims(refreshToken, REFRESH)

        if (getTokenType(claims) != REFRESH) {
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

    fun parseAdminToken(accessToken: String): Authentication {
        val claims = getAdminClaims(accessToken)

        if (getTokenType(claims) != ADMIN_ACCESS) {
            throw AdminInvalidTokenException()
        }

        val username = getAdminUsername(claims)
        return UsernamePasswordAuthenticationToken(
            username,
            null,
            listOf(SimpleGrantedAuthority(ROLE_ADMIN)),
        )
    }

    private fun getAdminClaims(token: String) =
        try {
            jwtParser.parseEncryptedClaims(token)
        } catch (e: ExpiredJwtException) {
            throw AdminExpiredTokenException()
        } catch (e: Exception) {
            logger.error("어드민 토큰 파싱 에러", e)
            throw AdminInvalidTokenException()
        }

    private fun getAdminUsername(claims: Jwe<Claims>): String =
        runCatching { claims.payload["username"] as? String ?: throw AdminInvalidTokenException() }
            .getOrElse { throw AdminInvalidTokenException() }

    private fun getClaims(
        token: String,
        tokenType: String,
    ) = try {
        jwtParser.parseEncryptedClaims(token)
    } catch (e: ExpiredJwtException) {
        when (tokenType) {
            ACCESS -> throw AuthenticationExpiredAccessTokenException()
            REFRESH -> throw AuthenticationExpiredRefreshTokenException()
            else -> throw AuthenticationInvalidTokenException()
        }
    } catch (e: Exception) {
        logger.error("토큰 파싱 에러 로그 token: $token", e)
        throw AuthenticationInvalidTokenException()
    }

    private fun getTokenType(claims: Jwe<Claims>): String =
        runCatching { claims.header[TOKEN_TYPE] as? String? ?: throw AuthenticationInvalidTokenException() }
            .getOrElse { throw AuthenticationInvalidTokenException() }

    private fun getUserId(claims: Jwe<Claims>): ObjectId =
        runCatching { ObjectId(claims.payload["user_id"] as? String ?: throw AuthenticationInvalidTokenException()) }
            .getOrElse { throw AuthenticationInvalidTokenException() }
}
