package notbe.tmtm.ddanddanserver.presentation.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import notbe.tmtm.ddanddanserver.domain.model.user.DailyInfo
import notbe.tmtm.ddanddanserver.domain.model.user.User

@Schema(description = "User & DailyInfo 응답 DTO")
data class UserDailyInfoResponse(
    @Schema(description = "User")
    val user: UserResponse,
    @Schema(description = "DailyInfo")
    val dailyInfo: DailyInfoResponse,
    @Schema(description = "지급한 먹이 수량")
    val rewardedFoodQuantity: Int,
    @Schema(description = "지급한 장난감 수량")
    val rewardedToyQuantity: Int,
) {
    companion object {
        fun fromDomain(
            user: User,
            dailyInfo: DailyInfo,
            rewardedFoodQuantity: Int,
            rewardedToyQuantity: Int,
        ) = UserDailyInfoResponse(
            user = UserResponse.fromDomain(user),
            dailyInfo = DailyInfoResponse.fromDomain(dailyInfo),
            rewardedFoodQuantity = rewardedFoodQuantity,
            rewardedToyQuantity = rewardedToyQuantity,
        )
    }
}
