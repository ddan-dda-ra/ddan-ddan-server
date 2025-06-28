package notbe.tmtm.ddanddanserver.application.processor

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import notbe.tmtm.ddanddanserver.domain.exception.AppleRestClientError
import notbe.tmtm.ddanddanserver.domain.exception.AppleTokenParseError
import notbe.tmtm.ddanddanserver.infrastructure.api.AppleAuthApi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

class AppleProcessorTest {
    private lateinit var appleAuthApi: AppleAuthApi
    private lateinit var objectMapper: ObjectMapper
    private lateinit var appleProcessor: AppleProcessor

    @BeforeEach
    fun setUp() {
        appleAuthApi = mockk()
        objectMapper = ObjectMapper()
        appleProcessor = AppleProcessor(appleAuthApi, objectMapper)
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
}