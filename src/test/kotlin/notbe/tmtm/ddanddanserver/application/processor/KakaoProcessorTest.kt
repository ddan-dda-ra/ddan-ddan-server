package notbe.tmtm.ddanddanserver.application.processor

import io.mockk.every
import io.mockk.mockk
import notbe.tmtm.ddanddanserver.domain.exception.KakaoParseError
import notbe.tmtm.ddanddanserver.domain.exception.KakaoRestClientError
import notbe.tmtm.ddanddanserver.domain.exception.KakaoUnauthorizedError
import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType
import notbe.tmtm.ddanddanserver.infrastructure.api.KakaoAuthApi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import kotlin.test.assertEquals

class KakaoProcessorTest {
    private lateinit var kakaoAuthApi: KakaoAuthApi
    private lateinit var kakaoProcessor: KakaoProcessor

    @BeforeEach
    fun setUp() {
        kakaoAuthApi = mockk()
        kakaoProcessor = KakaoProcessor(kakaoAuthApi)
    }

    @Test
    fun `정상적인 Kakao OAuth 정보 반환이 성공해야 한다`() {
        // given
        val accessToken = "valid_access_token"
        val mockResponse = KakaoAuthApi.KakaoUserInfoResponse(
            id = "12345",
            properties = KakaoAuthApi.KakaoUserInfoResponse.Properties(nickname = "테스트유저")
        )

        every { kakaoAuthApi.getUserInfo("Bearer $accessToken") } returns mockResponse

        // when
        val result = kakaoProcessor.getOAuth(accessToken)

        // then
        assertEquals("12345", result.id)
        assertEquals(OAuthType.KAKAO, result.type)
        assertEquals("테스트유저", result.nickName)
    }

    @Test
    fun `Kakao ID가 빈 문자열일 때 KakaoParseError를 던져야 한다`() {
        // given
        val accessToken = "valid_access_token"
        val mockResponse = KakaoAuthApi.KakaoUserInfoResponse(
            id = "",
            properties = KakaoAuthApi.KakaoUserInfoResponse.Properties(nickname = "테스트유저")
        )

        every { kakaoAuthApi.getUserInfo("Bearer $accessToken") } returns mockResponse

        // when & then
        assertThrows<KakaoParseError> {
            kakaoProcessor.getOAuth(accessToken)
        }
    }

    @Test
    fun `Kakao ID가 공백일 때 KakaoParseError를 던져야 한다`() {
        // given
        val accessToken = "valid_access_token"
        val mockResponse = KakaoAuthApi.KakaoUserInfoResponse(
            id = "   ",
            properties = KakaoAuthApi.KakaoUserInfoResponse.Properties(nickname = "테스트유저")
        )

        every { kakaoAuthApi.getUserInfo("Bearer $accessToken") } returns mockResponse

        // when & then
        assertThrows<KakaoParseError> {
            kakaoProcessor.getOAuth(accessToken)
        }
    }

    @Test
    fun `properties가 null일 때 ID를 nickname으로 사용해야 한다`() {
        // given
        val accessToken = "valid_access_token"
        val mockResponse = KakaoAuthApi.KakaoUserInfoResponse(
            id = "12345",
            properties = null
        )

        every { kakaoAuthApi.getUserInfo("Bearer $accessToken") } returns mockResponse

        // when
        val result = kakaoProcessor.getOAuth(accessToken)

        // then
        assertEquals("12345", result.id)
        assertEquals(OAuthType.KAKAO, result.type)
        assertEquals("12345", result.nickName) // ID를 nickname으로 사용
    }

    @Test
    fun `nickname이 null일 때 ID를 nickname으로 사용해야 한다`() {
        // given
        val accessToken = "valid_access_token"
        val mockResponse = KakaoAuthApi.KakaoUserInfoResponse(
            id = "12345",
            properties = KakaoAuthApi.KakaoUserInfoResponse.Properties(nickname = null)
        )

        every { kakaoAuthApi.getUserInfo("Bearer $accessToken") } returns mockResponse

        // when
        val result = kakaoProcessor.getOAuth(accessToken)

        // then
        assertEquals("12345", result.id)
        assertEquals(OAuthType.KAKAO, result.type)
        assertEquals("12345", result.nickName) // ID를 nickname으로 사용
    }

    @Test
    fun `HttpClientErrorException 발생 시 KakaoUnauthorizedError를 던져야 한다`() {
        // given
        val accessToken = "invalid_access_token"
        val clientException = HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Unauthorized")

        every { kakaoAuthApi.getUserInfo("Bearer $accessToken") } throws clientException

        // when & then
        assertThrows<KakaoUnauthorizedError> {
            kakaoProcessor.getOAuth(accessToken)
        }
    }

    @Test
    fun `일반 Exception 발생 시 KakaoRestClientError를 던져야 한다`() {
        // given
        val accessToken = "some_access_token"
        val exception = RuntimeException("네트워크 오류")

        every { kakaoAuthApi.getUserInfo("Bearer $accessToken") } throws exception

        // when & then
        assertThrows<KakaoRestClientError> {
            kakaoProcessor.getOAuth(accessToken)
        }
    }
}