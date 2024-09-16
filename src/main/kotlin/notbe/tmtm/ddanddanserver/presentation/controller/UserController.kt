package notbe.tmtm.ddanddanserver.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import notbe.tmtm.ddanddanserver.domain.usecase.user.GetUser
import notbe.tmtm.ddanddanserver.domain.usecase.user.UpdateCalorieAndRewardFood
import notbe.tmtm.ddanddanserver.domain.usecase.user.UpdateUser
import notbe.tmtm.ddanddanserver.presentation.dto.request.CalorieRequest
import notbe.tmtm.ddanddanserver.presentation.dto.request.UserRequest
import notbe.tmtm.ddanddanserver.presentation.dto.response.UserDailyInfoResponse
import notbe.tmtm.ddanddanserver.presentation.dto.response.UserResponse
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/v1/user")
@Tag(name = "유저")
class UserController(
    val getUser: GetUser,
    val updateUser: UpdateUser,
    val updateCalorieAndRewardFood: UpdateCalorieAndRewardFood,
) {
    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "내 정보를 조회합니다.")
    fun getMyInfo(authentication: Authentication): UserResponse {
        val result =
            getUser.execute(
                GetUser.GetUserInput(
                    userId = authentication.name,
                ),
            )
        return UserResponse.fromDomain(result.user)
    }

    @PutMapping("/me")
    @Operation(summary = "내 정보 수정", description = "내 정보를 수정합니다.")
    fun updateMyInfo(
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

    @PatchMapping("/me/daily-calorie")
    @Operation(summary = "일일 칼로리 갱신 & 먹이 지급", description = "일일 칼로리를 갱신하고 먹이를 지급합니다.")
    fun updateDailyCalorieAndRewardFood(
        authentication: Authentication,
        @RequestBody request: CalorieRequest,
    ): UserDailyInfoResponse {
        val result =
            updateCalorieAndRewardFood.execute(
                UpdateCalorieAndRewardFood.Input(
                    userId = authentication.name,
                    calorie = request.calorie,
                    today = LocalDate.now(),
                ),
            )
        return UserDailyInfoResponse.fromDomain(result.user, result.dailyInfo)
    }
}
