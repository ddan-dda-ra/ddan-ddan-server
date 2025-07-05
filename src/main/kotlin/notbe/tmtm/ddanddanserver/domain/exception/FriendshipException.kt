package notbe.tmtm.ddanddanserver.domain.exception

open class FriendshipException(
    errorCode: ErrorCode,
    data: Any? = null,
) : CustomException(errorCode, data)

class FriendshipNotFoundException : FriendshipException(ErrorCode.FRIEND_NOT_FOUND)

class FriendshipAlreadyExistsException : FriendshipException(ErrorCode.FRIEND_ALREADY_EXISTS)

class FriendshipSelfAddException : FriendshipException(ErrorCode.FRIEND_SELF_ADD)
