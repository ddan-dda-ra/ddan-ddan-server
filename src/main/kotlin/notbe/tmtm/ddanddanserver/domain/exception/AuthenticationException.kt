package notbe.tmtm.ddanddanserver.domain.exception

import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType

open class AuthenticationException : CustomException {
    constructor(errorCode: ErrorCode) : super(errorCode)
    constructor(errorCode: ErrorCode, data: Any? = null) : super(errorCode, data)
}

class OAuthenticationInvalidTokenException(
    oAuthType: OAuthType,
) : AuthenticationException(ErrorCode.INVALID_OAUTH_TOKEN, oAuthType)

class AuthenticationInvalidTokenException : AuthenticationException(ErrorCode.INVALID_AUTH_TOKEN)

class AuthenticationExpiredAccessTokenException : AuthenticationException(ErrorCode.EXPIRED_ACCESS_TOKEN)

class AuthenticationExpiredRefreshTokenException : AuthenticationException(ErrorCode.EXPIRED_REFRESH_TOKEN)

class AuthenticationTokenNotExistException : AuthenticationException(ErrorCode.NOT_EXIST_TOKEN)
