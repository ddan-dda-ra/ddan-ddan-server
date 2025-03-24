package notbe.tmtm.ddanddanserver.domain.exception

open class RankingException(
    errorCode: ErrorCode,
    data: Any? = null,
) : CustomException(errorCode, data)

class NotFoundUserStatException(
    data: Any? = null,
) : RankingException(ErrorCode.NOT_FOUND_USER_STAT, data)
