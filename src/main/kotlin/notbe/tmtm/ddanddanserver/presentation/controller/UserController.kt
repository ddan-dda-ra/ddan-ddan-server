package notbe.tmtm.ddanddanserver.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import notbe.tmtm.ddanddanserver.application.service.DailyInfoService
import notbe.tmtm.ddanddanserver.application.service.UserService
import notbe.tmtm.ddanddanserver.presentation.dto.request.CalorieRequest
import notbe.tmtm.ddanddanserver.presentation.dto.request.SetMainPetRequest
import notbe.tmtm.ddanddanserver.presentation.dto.request.UserRequest
import notbe.tmtm.ddanddanserver.presentation.dto.request.WithDrawRequest
import notbe.tmtm.ddanddanserver.presentation.dto.response.UserDailyInfoResponse
import notbe.tmtm.ddanddanserver.presentation.dto.response.UserMainPetResponse
import notbe.tmtm.ddanddanserver.presentation.dto.response.UserResponse
import org.bson.types.ObjectId
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/v1/users")
@Tag(name = "유저")
class UserController(
    private val userService: UserService,
    private val dailyInfoService: DailyInfoService,
) {
    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "내 정보를 조회합니다.")
    fun getMyInfo(authentication: Authentication): UserResponse {
        val userId = ObjectId(authentication.name)
        val user = userService.getByIdOrThrow(userId)
        return UserResponse.fromDomain(user)
    }

    @PutMapping("/me")
    @Operation(summary = "내 정보 수정", description = "내 정보를 수정합니다.")
    fun updateMyInfo(
        authentication: Authentication,
        @RequestBody request: UserRequest,
    ): UserResponse {
        val userId = ObjectId(authentication.name)
        val user = userService.updateInfo(userId, request.name, request.purposeCalorie)

        // 데일리 데이터에 이름 갱신
        dailyInfoService.getByUserIdAndDate(user.id, LocalDate.now())?.let {
            it.userName = user.name
            dailyInfoService.update(it)
        }

        return UserResponse.fromDomain(user)
    }

    @DeleteMapping("/me")
    @Operation(summary = "회원 탈퇴", description = "회원 탈퇴합니다.")
    fun deleteMyInfo(
        authentication: Authentication,
        @RequestBody request: WithDrawRequest,
    ) {
        val userId = ObjectId(authentication.name)
        userService.withdraw(userId)
        ResponseEntity.noContent()
    }

    @PatchMapping("/me/daily-calorie")
    @Operation(summary = "일일 칼로리 갱신 & 먹이 지급", description = "일일 칼로리를 갱신하고 먹이를 지급합니다.")
    fun updateDailyCalorieAndRewardFood(
        authentication: Authentication,
        @RequestBody request: CalorieRequest,
    ): UserDailyInfoResponse {
        val result =
            userService.updateCalorieAndRewardFood(
                userId = ObjectId(authentication.name),
                calorie = request.calorie,
                today = LocalDate.now(),
            )
        return UserDailyInfoResponse.fromDomain(
            result.user,
            result.dailyInfo,
            result.rewardedFoodQuantity,
            result.rewardedToyQuantity,
        )
    }

    @PostMapping("/me/main-pet")
    @Operation(summary = "메인 펫 설정", description = "메인 펫을 설정합니다.")
    fun setMainPet(
        authentication: Authentication,
        @RequestBody request: SetMainPetRequest,
    ): UserMainPetResponse {
        val result =
            userService.setMainPet(
                ownerUserId = ObjectId(authentication.name),
                petId = ObjectId(request.petId),
            )
        return UserMainPetResponse.fromDomain(result)
    }

    @GetMapping("/me/main-pet")
    @Operation(summary = "메인 펫 조회", description = "메인 펫을 조회합니다.")
    fun getMainPet(authentication: Authentication): UserMainPetResponse {
        val result =
            userService.getMainPet(
                userId = ObjectId(authentication.name),
            )
        return UserMainPetResponse.fromDomain(result)
    }
}
