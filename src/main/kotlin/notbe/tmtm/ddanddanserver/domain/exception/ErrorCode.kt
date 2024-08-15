package notbe.tmtm.ddanddanserver.domain.exception

enum class ErrorCode(
    val code: String,
    val message: String,
) {
    /**
     * 기본 오류
     * @see notbe.tmtm.ddanddanserver.domain.exception.DefaultException
     */
    UNKNOWN_SERVER_ERROR("DE0001", "알 수 없는 오류가 발생했습니다"),
    UNAUTHORIZED("DE0002", "인가되지 않은 접근입니다"),
    INVALID_INPUT("DE0003", "입력값(바디 혹은 파라미터)가 누락되었습니다"),
    UNKNOWN_RESOURCE("DE0004", "해당 리소스를 찾을 수 없습니다"),
    INVALID_METHOD("DE0005", "요청 메서드가 잘못되었습니다."),

    /**
     * 인가 오류
     * @see notbe.tmtm.ddanddanserver.domain.exception.AuthenticationException
     */
    INVALID_OAUTH_TOKEN("AC001", "OAuth 토큰 인증에 실패하였습니다."),
    INVALID_AUTH_TOKEN("AC002", "유효하지 않은 토큰입니다."),
    EXPIRED_ACCESS_TOKEN("AC003", "만료된 엑세스 토큰입니다."),
    EXPIRED_REFRESH_TOKEN("AC004", "만료된 리프레시 토큰입니다."),
    NOT_EXIST_TOKEN("AC005", "토큰이 존재하지 않습니다."),

    /**
     * 권한 오류
     * @see notbe.tmtm.ddanddanserver.domain.exception.AuthorizationException
     */
    PERMISSION_DENIED("AZ001", "권한이 없습니다."),

    /**
     * 카카오 OAuth 인증 오류
     * @see notbe.tmtm.ddanddanserver.domain.exception.KakaoOAuthException
     */
    KAKAO_RESPONSE_PARSE_ERROR("KO001", "카카오 응답을 파싱하는데 실패하였습니다."),
    KAKAO_REST_CLIENT_ERROR("KO002", "카카오 API 요청 중 오류가 발생하였습니다."),
}
