package notbe.tmtm.ddanddanserver.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import notbe.tmtm.ddanddanserver.domain.usecase.user.UpdateUser
import notbe.tmtm.ddanddanserver.presentation.dto.request.UserRequest
import notbe.tmtm.ddanddanserver.presentation.dto.response.UserResponse
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/user")
@Tag(name = "유저")
class UserController(
    val updateUser: UpdateUser,
) {
    @PutMapping("/me")
    @Operation(summary = "내 정보 수정", description = "내 정보를 수정합니다.")
    fun update(
        authentication: Authentication,
        @RequestBody request: UserRequest,
    ): UserResponse {
        val result =
            updateUser.execute(
                UpdateUser.UpdateUserInput(
                    userId = authentication.name,
                    name = request.name,
                    purposeCalorie = request.purposeCalorie,
                ),
            )

        return UserResponse.fromDomain(result.user)
    }
}
