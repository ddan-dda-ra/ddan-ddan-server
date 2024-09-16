package notbe.tmtm.ddanddanserver.domain.exception

open class PetException(
    errorCode: ErrorCode,
    data: Any? = null,
) : CustomException(errorCode, data)

class PetNotFoundException(
    data: Any? = null,
) : PetException(ErrorCode.PET_NOT_FOUND, data)

class PetOwnerMismatchException(
    data: Any? = null,
) : PetException(ErrorCode.PET_OWNER_MISMATCH, data)

class PetMaxLevelException(
    data: Any? = null,
) : PetException(ErrorCode.PET_MAX_LEVEL, data)
