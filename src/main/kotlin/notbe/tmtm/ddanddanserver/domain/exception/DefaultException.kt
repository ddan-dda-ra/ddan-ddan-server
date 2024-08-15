package notbe.tmtm.ddanddanserver.domain.exception

open class DefaultException(
    errorCode: ErrorCode,
) : CustomException(errorCode)

class UnauthorizedException : DefaultException(ErrorCode.UNAUTHORIZED)
