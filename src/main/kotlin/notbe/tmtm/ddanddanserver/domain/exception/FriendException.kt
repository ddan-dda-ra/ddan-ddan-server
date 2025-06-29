package notbe.tmtm.ddanddanserver.domain.exception

open class FriendException(
    errorCode: ErrorCode,
    data: Any? = null,
) : CustomException(errorCode, data)

class FriendNotFoundException(
    data: Any? = null,
) : FriendException(ErrorCode.FRIEND_NOT_FOUND, data)

class FriendAlreadyExistsException(
    data: Any? = null,
) : FriendException(ErrorCode.FRIEND_ALREADY_EXISTS, data)

class FriendSelfAddException(
    data: Any? = null,
) : FriendException(ErrorCode.FRIEND_SELF_ADD, data)

class FriendRequestNotFoundException(
    data: Any? = null,
) : FriendException(ErrorCode.FRIEND_REQUEST_NOT_FOUND, data)

class FriendRequestAlreadyProcessedException(
    data: Any? = null,
) : FriendException(ErrorCode.FRIEND_REQUEST_ALREADY_PROCESSED, data)
