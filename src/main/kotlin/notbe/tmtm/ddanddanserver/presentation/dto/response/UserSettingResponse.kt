package notbe.tmtm.ddanddanserver.presentation.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import notbe.tmtm.ddanddanserver.domain.model.user.UserSetting

@Schema(description = "UserSetting 응답 DTO")
data class UserSettingResponse(
    @Schema(description = "앱 푸시 여부", example = "true")
    val isAppPushOn: Boolean,
) {
    companion object {
        fun fromDomain(userSetting: UserSetting) =
            with(userSetting) {
                UserSettingResponse(
                    isAppPushOn = isAppPushOn,
                )
            }
    }
}
