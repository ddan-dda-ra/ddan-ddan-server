package notbe.tmtm.ddanddanserver.presentation.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "UserSetting 요청 DTO")
data class UserSettingRequest(
    @Schema(description = "앱 푸시 여부", example = "true")
    val isAppPushOn: Boolean?,
) {
    init {
        requireNotNull(isAppPushOn)
    }
}
