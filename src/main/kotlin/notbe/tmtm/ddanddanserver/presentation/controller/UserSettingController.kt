package notbe.tmtm.ddanddanserver.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import notbe.tmtm.ddanddanserver.application.service.UserService
import notbe.tmtm.ddanddanserver.presentation.dto.request.UserSettingRequest
import notbe.tmtm.ddanddanserver.presentation.dto.response.UserSettingResponse
import org.bson.types.ObjectId
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/users/me/settings")
@Tag(name = "유저")
class UserSettingController(
    private val userService: UserService,
) {
    @PatchMapping
    @Operation(summary = "유저 설정 수정", description = "유저 설정을 수정합니다.")
    fun updateUserSetting(
        authentication: Authentication,
        @RequestBody request: UserSettingRequest,
    ): UserSettingResponse {
        val userSetting = userService.updateSetting(
            userId = ObjectId(authentication.name),
            isAppPushOn = request.isAppPushOn,
        )
        return UserSettingResponse.fromDomain(userSetting)
    }
}
