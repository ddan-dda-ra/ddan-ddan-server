package notbe.tmtm.ddanddanserver.infrastructure.client

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import notbe.tmtm.ddanddanserver.infrastructure.client.dto.response.AppleOAuthInfoResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.math.BigInteger
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.Base64

@Component
class AppleClient(
    @Value("\${app.apple.client-id}") clientSecret: String,
//    @Value("\${APPLE_BUNDLE_ID}") bundleId: String,
    @Value("\${app.apple.team-id}") serviceId: String,
    @Value("\${app.apple.key-id}") keyId: String,
    private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
) {
    fun getOAuthInfo(token: String): AppleOAuthInfoResponse {
        val headers = parseHeaders(token)
        val appleKeys = getAppleKeys()
        val publicKey = generatePublicKey(headers, appleKeys)

        val claims = parseClaims(token, publicKey)
        return AppleOAuthInfoResponse(
            id = claims["sub"].toString(),
            properties =
                AppleOAuthInfoResponse.Properties(
                    nickname = claims["email"].toString(),
                ),
        )
    }

    private fun parseHeaders(token: String): Map<String, String> {
        val encodedHeader: String =
            token
                .split(TOKEN_VALUE_DELIMITER.toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()[0]
        val decodedHeader = String(Base64.getUrlDecoder().decode(encodedHeader))
        return objectMapper.readValue(
            decodedHeader,
            object : TypeReference<Map<String, String>>() {},
        )
    }

    private fun getAppleKeys(): AppleKeys =
        restClient
            .get()
            .uri("https://appleid.apple.com/auth/keys")
            .exchange { _, clientResponse ->
                clientResponse.bodyTo(AppleKeys::class.java)
                    ?: throw IllegalStateException("Apple 로그인 과정중 문제 발생. public key를 조회할 수 없음. $clientResponse")
            }

    private fun generatePublicKey(
        tokenHeaders: Map<String, String>,
        appleKeys: AppleKeys,
    ): PublicKey {
        val publicKeys: List<Key> = appleKeys.keys
        val publicKey: Key =
            publicKeys
                .stream()
                .filter { key -> key.alg == tokenHeaders["alg"] }
                .filter { key -> key.kid == tokenHeaders["kid"] }
                .findAny()
                .orElseThrow()

        return generatePublicKeyWithApplePublicKey(publicKey)
    }

    private fun generatePublicKeyWithApplePublicKey(applePublicKey: Key): PublicKey {
        val n = Base64.getDecoder().decode(applePublicKey.n)
        val e = Base64.getDecoder().decode(applePublicKey.e)

        val publicKeySpec =
            RSAPublicKeySpec(BigInteger(1, n), BigInteger(1, e))

        val keyFactory: KeyFactory = KeyFactory.getInstance(applePublicKey.kty)

        return keyFactory.generatePublic(publicKeySpec)
    }

    private fun parseClaims(
        idToken: String?,
        publicKey: PublicKey?,
    ): Claims =
        Jwts
            .parser()
            .verifyWith(publicKey)
            .build()
            .parseEncryptedClaims(idToken)
            .payload

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
