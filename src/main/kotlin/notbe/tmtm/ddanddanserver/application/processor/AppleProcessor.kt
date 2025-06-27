package notbe.tmtm.ddanddanserver.application.processor

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType
import notbe.tmtm.ddanddanserver.infrastructure.api.AppleAuthApi
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.Base64

@Component
class AppleProcessor(
    private val appleAuthApi: AppleAuthApi,
    private val objectMapper: ObjectMapper,
) : OAuthProcessor {
    override fun getOAuth(accessToken: String): OAuthInfo {
        val headers = parseHeaders(accessToken)
        val appleKeys = getAppleKeys()
        val publicKey = generatePublicKey(headers, appleKeys)
        val claims = parseClaims(accessToken, publicKey)

        return OAuthInfo(
            id = claims["sub"].toString(),
            type = this.getProviderType(),
            nickName = claims["email"].toString(),
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
        val n = Base64.getUrlDecoder().decode(applePublicKey.n)
        val e = Base64.getUrlDecoder().decode(applePublicKey.e)

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
            .parseSignedClaims(idToken)
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
