package notbe.tmtm.ddanddanserver.application.processor

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import notbe.tmtm.ddanddanserver.common.util.logger
import notbe.tmtm.ddanddanserver.domain.exception.AppleKeyGenerationError
import notbe.tmtm.ddanddanserver.domain.exception.AppleRestClientError
import notbe.tmtm.ddanddanserver.domain.exception.AppleTokenParseError
import notbe.tmtm.ddanddanserver.domain.exception.AppleTokenValidationError
import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType
import notbe.tmtm.ddanddanserver.infrastructure.api.AppleAuthApi
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.*

@Component
@ConditionalOnProperty(prefix = "mock-oauth", name = ["enabled"], havingValue = "false", matchIfMissing = true)
class AppleProcessor(
    private val appleAuthApi: AppleAuthApi,
    private val objectMapper: ObjectMapper,
) : OAuthProcessor {
    override fun getProviderType(): OAuthType = OAuthType.APPLE

    override fun getOAuth(accessToken: String): OAuth {
        val headers = parseHeaders(accessToken)
        val appleKeys = getAppleKeys()
        val publicKey = generatePublicKey(headers, appleKeys)
        val claims = parseClaims(accessToken, publicKey)

        return OAuth(
            id = claims["sub"].toString(),
            type = this.getProviderType(),
            nickName = claims["email"].toString(),
        )
    }

    private fun parseHeaders(token: String): Map<String, String> {
        return try {
            val encodedHeader: String =
                token
                    .split(TOKEN_VALUE_DELIMITER.toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()[0]
            val decodedHeader = String(Base64.getUrlDecoder().decode(encodedHeader))
            objectMapper.readValue(
                decodedHeader,
                object : TypeReference<Map<String, String>>() {},
            )
        } catch (exception: Exception) {
            logger().error("Apple 토큰 헤더 파싱 중 오류가 발생했습니다", exception)
            throw AppleTokenParseError(exception.message)
        }
    }

    private fun getAppleKeys(): AppleKeys =
        try {
            AppleKeys(
                keys =
                    appleAuthApi.getPublicKey().keys.map { keys ->
                        Key(
                            kty = keys.kty,
                            kid = keys.kid,
                            use = keys.use,
                            alg = keys.alg,
                            n = keys.n,
                            e = keys.e,
                        )
                    },
            )
        } catch (exception: Exception) {
            logger().error("Apple 공개키 조회 중 오류가 발생했습니다", exception)
            throw AppleRestClientError(exception.message)
        }

    private fun generatePublicKey(
        tokenHeaders: Map<String, String>,
        appleKeys: AppleKeys,
    ): PublicKey {
        return try {
            val publicKeys: List<Key> = appleKeys.keys
            val publicKey: Key =
                publicKeys
                    .stream()
                    .filter { key -> key.alg == tokenHeaders["alg"] }
                    .filter { key -> key.kid == tokenHeaders["kid"] }
                    .findAny()
                    .orElseThrow {
                        logger().error("일치하는 Apple 공개키를 찾을 수 없습니다. alg: ${tokenHeaders["alg"]}, kid: ${tokenHeaders["kid"]}")
                        AppleKeyGenerationError("일치하는 Apple 공개키를 찾을 수 없습니다")
                    }

            generatePublicKeyWithApplePublicKey(publicKey)
        } catch (exception: AppleKeyGenerationError) {
            throw exception
        } catch (exception: Exception) {
            logger().error("Apple 공개키 생성 중 오류가 발생했습니다", exception)
            throw AppleKeyGenerationError(exception.message)
        }
    }

    private fun generatePublicKeyWithApplePublicKey(applePublicKey: Key): PublicKey {
        return try {
            val n = Base64.getUrlDecoder().decode(applePublicKey.n)
            val e = Base64.getUrlDecoder().decode(applePublicKey.e)

            val publicKeySpec =
                RSAPublicKeySpec(BigInteger(1, n), BigInteger(1, e))

            val keyFactory: KeyFactory = KeyFactory.getInstance(applePublicKey.kty)

            keyFactory.generatePublic(publicKeySpec)
        } catch (exception: Exception) {
            logger().error("Apple RSA 공개키 생성 중 오류가 발생했습니다", exception)
            throw AppleKeyGenerationError(exception.message)
        }
    }

    private fun parseClaims(
        idToken: String?,
        publicKey: PublicKey?,
    ): Claims =
        try {
            Jwts
                .parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(idToken)
                .payload
        } catch (exception: Exception) {
            logger().error("Apple 토큰 검증 중 오류가 발생했습니다", exception)
            throw AppleTokenValidationError(exception.message)
        }

    data class AppleKeys(
        val keys: List<Key>,
    )

    data class Key(
        val kty: String, // Key Type (e.g., "RSA")
        val kid: String, // Key ID
        val use: String, // Usage (e.g., "sig")
        val alg: String, // Algorithm (e.g., "RS256")
        val n: String, // Modulus
        val e: String, // Exponent
    )

    private companion object {
        private const val TOKEN_VALUE_DELIMITER = "\\."
    }
}
