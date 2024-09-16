package notbe.tmtm.ddanddanserver.presentation.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import notbe.tmtm.ddanddanserver.domain.model.DailyInfo
import notbe.tmtm.ddanddanserver.domain.model.User

@Schema(description = "User & DailyInfo 응답 DTO")
data class UserDailyInfoResponse(
    @Schema(description = "User")
    val user: UserResponse,
    @Schema(description = "DailyInfo")
    val dailyInfo: DailyInfoResponse,
) {
    companion object {
        fun fromDomain(
            user: User,
            dailyInfo: DailyInfo,
        ) = UserDailyInfoResponse(
            user = UserResponse.fromDomain(user),
            dailyInfo = DailyInfoResponse.fromDomain(dailyInfo),
        )
    }
}
