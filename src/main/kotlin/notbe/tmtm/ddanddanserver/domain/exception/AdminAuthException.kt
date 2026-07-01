package notbe.tmtm.ddanddanserver.domain.exception

open class AdminAuthException(
    errorCode: ErrorCode,
    data: Any? = null,
) : CustomException(errorCode, data)

class AdminUnauthorizedException(
    data: Any? = null,
) : AdminAuthException(ErrorCode.ADMIN_UNAUTHORIZED, data)

class AdminInvalidTokenException(
    data: Any? = null,
) : AdminAuthException(ErrorCode.ADMIN_INVALID_TOKEN, data)

class AdminExpiredTokenException(
    data: Any? = null,
) : AdminAuthException(ErrorCode.ADMIN_EXPIRED_TOKEN, data)
