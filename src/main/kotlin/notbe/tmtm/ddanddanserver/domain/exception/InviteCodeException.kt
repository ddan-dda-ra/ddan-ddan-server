package notbe.tmtm.ddanddanserver.domain.exception

open class InviteCodeException(
    errorCode: ErrorCode,
    data: Any? = null,
) : CustomException(errorCode, data)

class InviteCodeNotFoundException : InviteCodeException(ErrorCode.INVITE_CODE_NOT_FOUND)

class InviteCodeExpiredException : InviteCodeException(ErrorCode.INVITE_CODE_EXPIRED)

class InviteCodeInvalidException : InviteCodeException(ErrorCode.INVITE_CODE_INVALID)

class InviteCodeSelfUseException : InviteCodeException(ErrorCode.INVITE_CODE_SELF_USE)
