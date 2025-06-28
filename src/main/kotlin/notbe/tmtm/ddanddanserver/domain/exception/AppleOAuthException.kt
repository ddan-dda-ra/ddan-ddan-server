package notbe.tmtm.ddanddanserver.domain.exception

open class AppleOAuthException(
    errorCode: ErrorCode,
    data: Any? = null,
) : CustomException(errorCode, data)

class AppleTokenParseError(
    data: Any? = null,
) : AppleOAuthException(ErrorCode.APPLE_TOKEN_PARSE_ERROR, data)

class AppleKeyGenerationError(
    data: Any? = null,
) : AppleOAuthException(ErrorCode.APPLE_KEY_GENERATION_ERROR, data)

class AppleTokenValidationError(
    data: Any? = null,
) : AppleOAuthException(ErrorCode.APPLE_TOKEN_VALIDATION_ERROR, data)

class AppleRestClientError(
    data: Any? = null,
) : AppleOAuthException(ErrorCode.APPLE_REST_CLIENT_ERROR, data)