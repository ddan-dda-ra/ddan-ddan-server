package notbe.tmtm.ddanddanserver.domain.exception

open class AppVersionException(
    errorCode: ErrorCode,
    data: Any? = null,
) : CustomException(
    errorCode = errorCode,
    data = data,
)

class AppVersionUpgradeRequiredException(
    platform: String,
    currentVersion: String,
    minimumVersion: String,
) : AppVersionException(
    errorCode = ErrorCode.APP_VERSION_UPGRADE_REQUIRED,
    data = mapOf(
        "platform" to platform,
        "currentVersion" to currentVersion,
        "minimumVersion" to minimumVersion,
    ),
)
