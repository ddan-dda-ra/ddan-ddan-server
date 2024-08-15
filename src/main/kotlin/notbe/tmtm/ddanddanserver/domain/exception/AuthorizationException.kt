package notbe.tmtm.ddanddanserver.domain.exception

open class AuthorizationException(
    errorCode: ErrorCode,
) : CustomException(errorCode)

class PermissionDeniedException : AuthorizationException(ErrorCode.PERMISSION_DENIED)
