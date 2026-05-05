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
    INVALID_INPUT("DE0003", "입력값(바디 혹은 파라미터)가 누락되거나, 잘못되었습니다."),
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
    KAKAO_UNAUTHORIZED_ERROR("KO003", "카카오 인증에 실패하였습니다."),

    /**
     * 애플 OAuth 인증 오류
     * @see notbe.tmtm.ddanddanserver.domain.exception.AppleOAuthException
     */
    APPLE_TOKEN_PARSE_ERROR("AO001", "애플 토큰을 파싱하는데 실패하였습니다."),
    APPLE_KEY_GENERATION_ERROR("AO002", "애플 공개키 생성에 실패하였습니다."),
    APPLE_TOKEN_VALIDATION_ERROR("AO003", "애플 토큰 검증에 실패하였습니다."),
    APPLE_REST_CLIENT_ERROR("AO004", "애플 API 요청 중 오류가 발생하였습니다."),

    /**
     * 유저 오류
     * @see notbe.tmtm.ddanddanserver.domain.exception.UserException
     */
    USER_NOT_FOUND("UE001", "유저를 찾을 수 없습니다."),
    USER_FOOD_QUANTITY_LACK("UE002", "먹이가 부족합니다."),
    USER_TOY_QUANTITY_LACK("UE003", "장난감이 부족합니다."),
    USER_TICKET_LACK("UE004", "티켓이 부족합니다."),

    /**
     * 펫 오류
     * @see notbe.tmtm.ddanddanserver.domain.exception.PetException
     */
    PET_NOT_FOUND("PE001", "펫을 찾을 수 없습니다."),
    PET_OWNER_MISMATCH("PE002", "펫의 소유자가 일치하지 않습니다."),
    PET_MAX_LEVEL("PE003", "펫이 최대 레벨입니다."),

    /**
     * 랭킹 오류
     * @see notbe.tmtm.ddanddanserver.domain.exception.RankingException
     */
    NOT_FOUND_USER_STAT("RA001", "유저의 랭킹 정보를 찾을 수 없습니다."),

    /**
     * 초대코드 오류
     * @see notbe.tmtm.ddanddanserver.domain.exception.InviteCodeException
     */
    INVITE_CODE_NOT_FOUND("IC001", "초대코드를 찾을 수 없습니다."),
    INVITE_CODE_EXPIRED("IC002", "만료된 초대코드입니다."),
    INVITE_CODE_INVALID("IC003", "유효하지 않은 초대코드입니다."),
    INVITE_CODE_SELF_USE("IC004", "자신의 초대코드는 사용할 수 없습니다."),

    /**
     * 친구 오류
     * @see notbe.tmtm.ddanddanserver.domain.exception.FriendshipException
     */
    FRIEND_NOT_FOUND("FR001", "친구를 찾을 수 없습니다."),
    FRIEND_ALREADY_EXISTS("FR002", "이미 친구입니다."),
    FRIEND_SELF_ADD("FR003", "자기 자신을 친구로 추가할 수 없습니다."),

    /**
     * 앱 버전 오류
     * @see notbe.tmtm.ddanddanserver.domain.exception.AppVersionException
     */
    APP_VERSION_UPGRADE_REQUIRED("AV001", "앱 업데이트가 필요합니다."),

    /**
     * 응원 오류
     * @see notbe.tmtm.ddanddanserver.domain.exception.CheerException
     */
    CHEER_ALREADY_EXISTS("CE001", "해당 유저에 대해 이미 응원했습니다."),
    CHEER_SELF("CE002", "자기 자신을 응원할 수 없습니다."),

    /**
     * 펫 카탈로그 오류
     * @see notbe.tmtm.ddanddanserver.domain.exception.PetCatalogException
     */
    PET_CATALOG_NOT_FOUND("PC001", "펫 카탈로그 항목을 찾을 수 없습니다."),
    PET_CATALOG_DUPLICATE_KEY("PC002", "이미 존재하는 펫 카탈로그 키입니다."),

    /**
     * 어드민 인증 오류
     * @see notbe.tmtm.ddanddanserver.domain.exception.AdminAuthException
     */
    ADMIN_INVALID_CREDENTIALS("AA001", "어드민 자격 증명이 올바르지 않습니다."),
    ADMIN_UNAUTHORIZED("AA002", "어드민 인증이 필요합니다."),
    ADMIN_INVALID_TOKEN("AA003", "유효하지 않은 어드민 토큰입니다."),
    ADMIN_EXPIRED_TOKEN("AA004", "만료된 어드민 토큰입니다."),
}
