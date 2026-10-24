package notbe.tmtm.ddanddanserver.application.processor

import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Jwts
import io.mockk.every
import io.mockk.mockk
import notbe.tmtm.ddanddanserver.config.AppleOAuthProperties
import notbe.tmtm.ddanddanserver.domain.exception.AppleRestClientError
import notbe.tmtm.ddanddanserver.domain.exception.AppleTokenParseError
import notbe.tmtm.ddanddanserver.domain.exception.AppleTokenValidationError
import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType
import notbe.tmtm.ddanddanserver.infrastructure.api.AppleAuthApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.util.*

class AppleProcessorTest {
    private lateinit var appleAuthApi: AppleAuthApi
    private lateinit var objectMapper: ObjectMapper
    private lateinit var appleProcessor: AppleProcessor

    @BeforeEach
    fun setUp() {
        appleAuthApi = mockk()
        objectMapper = ObjectMapper()
        appleProcessor =
            AppleProcessor(
                appleAuthApi,
                objectMapper,
                AppleOAuthProperties(clientIds = listOf(CLIENT_ID)),
            )
    }

    @Test
    fun `잘못된 JWT 토큰 형식일 때 AppleTokenParseError를 던져야 한다`() {
        // given
        val invalidToken = "invalid.jwt.token"

        // when & then
        assertThrows<AppleTokenParseError> {
            appleProcessor.getOAuth(invalidToken)
        }
    }

    @Test
    fun `토큰에 구분자가 없을 때 AppleTokenParseError를 던져야 한다`() {
        // given
        val invalidToken = "invalidtokenwithoutdots"

        // when & then
        assertThrows<AppleTokenParseError> {
            appleProcessor.getOAuth(invalidToken)
        }
    }

    @Test
    fun `Base64 디코딩 실패 시 AppleTokenParseError를 던져야 한다`() {
        // given
        val invalidToken = "invalid-base64.header.signature"

        // when & then
        assertThrows<AppleTokenParseError> {
            appleProcessor.getOAuth(invalidToken)
        }
    }

    @Test
    fun `Apple API 호출 실패 시 AppleRestClientError를 던져야 한다`() {
        // given
        val validJwtToken = createValidJwtToken()

        every { appleAuthApi.getPublicKey() } throws RuntimeException("API 호출 실패")

        // when & then
        assertThrows<AppleRestClientError> {
            appleProcessor.getOAuth(validJwtToken)
        }
    }

    private fun createValidJwtToken(): String {
        val header = """{"alg":"RS256","kid":"testKeyId"}"""
        val payload = """{"sub":"apple.user.12345","email":"test@example.com"}"""
        val signature = "signature"
        
        val encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(header.toByteArray())
        val encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        val encodedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(signature.toByteArray())
        
        return "$encodedHeader.$encodedPayload.$encodedSignature"
    }

    @Test
    fun `iss와 aud가 올바른 토큰은 검증에 성공해야 한다`() {
        // given
        val token = signedToken()
        every { appleAuthApi.getPublicKey() } returns applePublicKeyResponse()

        // when
        val oAuth = appleProcessor.getOAuth(token)

        // then
        assertEquals(SUBJECT, oAuth.id)
        assertEquals(OAuthType.APPLE, oAuth.type)
        assertEquals(EMAIL, oAuth.nickName)
    }

    @Test
    fun `iss가 Apple이 아닌 토큰은 AppleTokenValidationError를 던져야 한다`() {
        // given
        val token = signedToken(issuer = "https://evil.example.com")
        every { appleAuthApi.getPublicKey() } returns applePublicKeyResponse()

        // when & then
        assertThrows<AppleTokenValidationError> {
            appleProcessor.getOAuth(token)
        }
    }

    @Test
    fun `aud가 허용된 client id가 아닌 토큰은 AppleTokenValidationError를 던져야 한다`() {
        // given
        val token = signedToken(audience = "com.other.app")
        every { appleAuthApi.getPublicKey() } returns applePublicKeyResponse()

        // when & then
        assertThrows<AppleTokenValidationError> {
            appleProcessor.getOAuth(token)
        }
    }

    @Test
    fun `aud가 없는 토큰은 AppleTokenValidationError를 던져야 한다`() {
        // given
        val token = signedToken(audience = null)
        every { appleAuthApi.getPublicKey() } returns applePublicKeyResponse()

        // when & then
        assertThrows<AppleTokenValidationError> {
            appleProcessor.getOAuth(token)
        }
    }

    @Test
    fun `email 클레임이 없으면 닉네임을 문자열 null이 아닌 sub로 대체해야 한다`() {
        // given
        val token = signedToken(email = null)
        every { appleAuthApi.getPublicKey() } returns applePublicKeyResponse()

        // when
        val oAuth = appleProcessor.getOAuth(token)

        // then
        assertEquals(SUBJECT, oAuth.nickName)
    }

    private fun signedToken(
        issuer: String = APPLE_ISSUER,
        audience: String? = CLIENT_ID,
        email: String? = EMAIL,
    ): String {
        val builder =
            Jwts
                .builder()
                .header()
                .keyId(KEY_ID)
                .and()
                .issuer(issuer)
                .subject(SUBJECT)
                .issuedAt(Date())
                .expiration(Date(System.currentTimeMillis() + 60_000))
        audience?.let { builder.audience().add(it) }
        email?.let { builder.claim("email", it) }
        return builder.signWith(keyPair.private, Jwts.SIG.RS256).compact()
    }

    private fun applePublicKeyResponse(): AppleAuthApi.ApplePublicKeyResponse {
        val publicKey = keyPair.public as RSAPublicKey
        return AppleAuthApi.ApplePublicKeyResponse(
            keys =
                listOf(
                    AppleAuthApi.ApplePublicKeyResponse.KeyResponse(
                        kty = "RSA",
                        kid = KEY_ID,
                        use = "sig",
                        alg = "RS256",
                        n = base64Url(publicKey.modulus),
                        e = base64Url(publicKey.publicExponent),
                    ),
                ),
        )
    }

    private fun base64Url(value: BigInteger): String {
        val bytes = value.toByteArray().let { if (it[0] == 0.toByte()) it.copyOfRange(1, it.size) else it }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private companion object {
        const val APPLE_ISSUER = "https://appleid.apple.com"
        const val CLIENT_ID = "inc.ddanddan.DDanDDan"
        const val KEY_ID = "testKeyId"
        const val SUBJECT = "apple.user.12345"
        const val EMAIL = "test@example.com"

        val keyPair: KeyPair =
            KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    }
}
