package notbe.tmtm.ddanddanserver.common

import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest
import notbe.tmtm.ddanddanserver.domain.exception.*
import notbe.tmtm.ddanddanserver.presentation.dto.response.ErrorResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

class WebExceptionHandlerTest {
    private lateinit var webExceptionHandler: WebExceptionHandler
    private lateinit var httpServletRequest: HttpServletRequest

    @BeforeEach
    fun setUp() {
        webExceptionHandler = WebExceptionHandler()
        httpServletRequest = mockk(relaxed = true)
    }

    @Test
    fun `KakaoParseError 예외가 401 Unauthorized로 처리되어야 한다`() {
        // given
        val exception = KakaoParseError("파싱 오류")

        // when
        val response = webExceptionHandler.handleOAuthException(exception, httpServletRequest)

        // then
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals(ErrorCode.KAKAO_RESPONSE_PARSE_ERROR.code, response.body?.code)
        assertEquals(ErrorCode.KAKAO_RESPONSE_PARSE_ERROR.message, response.body?.message)
    }

    @Test
    fun `KakaoUnauthorizedError 예외가 401 Unauthorized로 처리되어야 한다`() {
        // given
        val exception = KakaoUnauthorizedError("인증 실패")

        // when
        val response = webExceptionHandler.handleOAuthException(exception, httpServletRequest)

        // then
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals(ErrorCode.KAKAO_UNAUTHORIZED_ERROR.code, response.body?.code)
        assertEquals(ErrorCode.KAKAO_UNAUTHORIZED_ERROR.message, response.body?.message)
    }

    @Test
    fun `KakaoRestClientError 예외가 401 Unauthorized로 처리되어야 한다`() {
        // given
        val exception = KakaoRestClientError("API 호출 오류")

        // when
        val response = webExceptionHandler.handleOAuthException(exception, httpServletRequest)

        // then
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals(ErrorCode.KAKAO_REST_CLIENT_ERROR.code, response.body?.code)
        assertEquals(ErrorCode.KAKAO_REST_CLIENT_ERROR.message, response.body?.message)
    }

    @Test
    fun `AppleTokenParseError 예외가 401 Unauthorized로 처리되어야 한다`() {
        // given
        val exception = AppleTokenParseError("토큰 파싱 오류")

        // when
        val response = webExceptionHandler.handleOAuthException(exception, httpServletRequest)

        // then
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals(ErrorCode.APPLE_TOKEN_PARSE_ERROR.code, response.body?.code)
        assertEquals(ErrorCode.APPLE_TOKEN_PARSE_ERROR.message, response.body?.message)
    }

    @Test
    fun `AppleKeyGenerationError 예외가 401 Unauthorized로 처리되어야 한다`() {
        // given
        val exception = AppleKeyGenerationError("키 생성 오류")

        // when
        val response = webExceptionHandler.handleOAuthException(exception, httpServletRequest)

        // then
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals(ErrorCode.APPLE_KEY_GENERATION_ERROR.code, response.body?.code)
        assertEquals(ErrorCode.APPLE_KEY_GENERATION_ERROR.message, response.body?.message)
    }

    @Test
    fun `AppleTokenValidationError 예외가 401 Unauthorized로 처리되어야 한다`() {
        // given
        val exception = AppleTokenValidationError("토큰 검증 오류")

        // when
        val response = webExceptionHandler.handleOAuthException(exception, httpServletRequest)

        // then
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals(ErrorCode.APPLE_TOKEN_VALIDATION_ERROR.code, response.body?.code)
        assertEquals(ErrorCode.APPLE_TOKEN_VALIDATION_ERROR.message, response.body?.message)
    }

    @Test
    fun `AppleRestClientError 예외가 401 Unauthorized로 처리되어야 한다`() {
        // given
        val exception = AppleRestClientError("API 호출 오류")

        // when
        val response = webExceptionHandler.handleOAuthException(exception, httpServletRequest)

        // then
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals(ErrorCode.APPLE_REST_CLIENT_ERROR.code, response.body?.code)
        assertEquals(ErrorCode.APPLE_REST_CLIENT_ERROR.message, response.body?.message)
    }

    @Test
    fun `CustomException은 400 Bad Request로 처리되어야 한다`() {
        // given
        val exception = object : CustomException(ErrorCode.INVALID_INPUT, "잘못된 입력") {}

        // when
        val response = webExceptionHandler.handleDomainException(exception, httpServletRequest)

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(ErrorCode.INVALID_INPUT.code, response.body?.code)
        assertEquals(ErrorCode.INVALID_INPUT.message, response.body?.message)
    }

    @Test
    fun `AuthenticationException은 401 Unauthorized로 처리되어야 한다`() {
        // given
        val exception = object : AuthenticationException(ErrorCode.INVALID_AUTH_TOKEN, "유효하지 않은 토큰") {}

        // when
        val response = webExceptionHandler.handleAuthenticationException(exception, httpServletRequest)

        // then
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals(ErrorCode.INVALID_AUTH_TOKEN.code, response.body?.code)
        assertEquals(ErrorCode.INVALID_AUTH_TOKEN.message, response.body?.message)
    }

    @Test
    fun `AuthorizationException은 403 Forbidden으로 처리되어야 한다`() {
        // given
        val exception = object : AuthorizationException(ErrorCode.PERMISSION_DENIED) {}

        // when
        val response = webExceptionHandler.handleAuthorizationException(exception)

        // then
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        assertEquals(ErrorCode.PERMISSION_DENIED.code, response.body?.code)
        assertEquals(ErrorCode.PERMISSION_DENIED.message, response.body?.message)
    }

    @Test
    fun `AppVersionException은 426 Upgrade Required로 처리되어야 한다`() {
        // given
        val exception = AppVersionUpgradeRequiredException(
            platform = "android",
            currentVersion = "1.0.0",
            minimumVersion = "2.0.0"
        )

        // when
        val response = webExceptionHandler.handleAppVersionException(exception, httpServletRequest)

        // then
        assertEquals(HttpStatus.UPGRADE_REQUIRED, response.statusCode)
        assertEquals(ErrorCode.APP_VERSION_UPGRADE_REQUIRED.code, response.body?.code)
        assertEquals(ErrorCode.APP_VERSION_UPGRADE_REQUIRED.message, response.body?.message)
    }
}
