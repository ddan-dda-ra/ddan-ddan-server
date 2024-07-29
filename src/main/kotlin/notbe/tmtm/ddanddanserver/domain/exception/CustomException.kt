package notbe.tmtm.ddanddanserver.domain.exception

open class CustomException(
    val errorCode: ErrorCode,
    val data: Any? = null,
) : RuntimeException(errorCode.message)
