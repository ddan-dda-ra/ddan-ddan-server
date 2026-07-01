package notbe.tmtm.ddanddanserver.domain.exception

open class PetCatalogException(
    errorCode: ErrorCode,
    data: Any? = null,
) : CustomException(errorCode, data)

class PetCatalogNotFoundException(
    data: Any? = null,
) : PetCatalogException(ErrorCode.PET_CATALOG_NOT_FOUND, data)

class PetCatalogInactiveException(
    data: Any? = null,
) : PetCatalogException(ErrorCode.PET_CATALOG_INACTIVE, data)

class PetCatalogInvalidException(
    data: Any? = null,
) : PetCatalogException(ErrorCode.PET_CATALOG_INVALID, data)
