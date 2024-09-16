package notbe.tmtm.ddanddanserver.domain.exception

open class DailyInfoException(
    errorCode: ErrorCode,
    data: Any? = null,
) : CustomException(errorCode, data)
