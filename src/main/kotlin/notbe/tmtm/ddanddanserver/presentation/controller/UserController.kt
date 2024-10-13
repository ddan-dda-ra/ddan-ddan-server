package notbe.tmtm.ddanddanserver.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import notbe.tmtm.ddanddanserver.domain.usecase.user.GetMainPet
import notbe.tmtm.ddanddanserver.domain.usecase.user.GetUser
import notbe.tmtm.ddanddanserver.domain.usecase.user.SetMainPet
import notbe.tmtm.ddanddanserver.domain.usecase.user.UpdateCalorieAndRewardFood
import notbe.tmtm.ddanddanserver.domain.usecase.user.UpdateUser
import notbe.tmtm.ddanddanserver.presentation.dto.request.CalorieRequest
import notbe.tmtm.ddanddanserver.presentation.dto.request.SetMainPetRequest
import notbe.tmtm.ddanddanserver.presentation.dto.request.UserRequest
import notbe.tmtm.ddanddanserver.presentation.dto.response.UserDailyInfoResponse
import notbe.tmtm.ddanddanserver.presentation.dto.response.UserMainPetResponse
import notbe.tmtm.ddanddanserver.presentation.dto.response.UserResponse
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/v1/user")
@Tag(name = "유저")
class UserController(
    val getUser: GetUser,
    val updateUser: UpdateUser,
    val updateCalorieAndRewardFood: UpdateCalorieAndRewardFood,
    val setMainPet: SetMainPet,
    val getMainPet: GetMainPet,
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

    @PostMapping("/me/main-pet")
    @Operation(summary = "메인 펫 설정", description = "메인 펫을 설정합니다.")
    fun setMainPet(
        authentication: Authentication,
        @RequestBody request: SetMainPetRequest,
    ): UserMainPetResponse {
        val result =
            setMainPet.execute(
                SetMainPet.Input(
                    ownerUserId = authentication.name,
                    petId = request.petId,
                ),
            )
        return UserMainPetResponse.fromDomain(result.pet)
    }

    @GetMapping("/me/main-pet")
    @Operation(summary = "메인 펫 조회", description = "메인 펫을 조회합니다.")
    fun getMainPet(authentication: Authentication): UserMainPetResponse {
        val result =
            getMainPet.execute(
                GetMainPet.Input(
                    userId = authentication.name,
                ),
            )
        return UserMainPetResponse.fromDomain(result.mainPet)
    }
}
