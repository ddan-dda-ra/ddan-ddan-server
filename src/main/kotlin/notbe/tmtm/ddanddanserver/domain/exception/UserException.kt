package notbe.tmtm.ddanddanserver.domain.exception

open class UserException(
    errorCode: ErrorCode,
    data: Any? = null,
) : CustomException(errorCode, data)

class UserNotFoundException(
    data: Any? = null,
) : UserException(ErrorCode.USER_NOT_FOUND, data)

class UserFoodQuantityLackException(
    data: Any? = null,
) : UserException(ErrorCode.USER_FOOD_QUANTITY_LACK, data)
