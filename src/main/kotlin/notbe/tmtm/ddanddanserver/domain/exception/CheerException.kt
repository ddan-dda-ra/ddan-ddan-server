package notbe.tmtm.ddanddanserver.domain.exception

open class CheerException(
    errorCode: ErrorCode,
    data: Any? = null,
) : CustomException(errorCode, data)

class CheerAlreadyExistsException(
    data: Any? = null,
) : CheerException(ErrorCode.CHEER_ALREADY_EXISTS, data)

class CheerSelfException(
    data: Any? = null,
) : CheerException(ErrorCode.CHEER_SELF, data)

class CheerNotFriendsException(
    data: Any? = null,
) : CheerException(ErrorCode.CHEER_NOT_FRIENDS, data)
