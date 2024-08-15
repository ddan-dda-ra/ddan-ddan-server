package notbe.tmtm.ddanddanserver.domain.exception

open class KakaoOAuthException(
    errorCode: ErrorCode,
    data: Any? = null,
) : CustomException(errorCode, data)

class KakaoParseError(
    data: Any? = null,
) : KakaoOAuthException(ErrorCode.KAKAO_RESPONSE_PARSE_ERROR, data)

class KakaoRestClientError(
    data: Any? = null,
) : KakaoOAuthException(ErrorCode.KAKAO_REST_CLIENT_ERROR, data)
