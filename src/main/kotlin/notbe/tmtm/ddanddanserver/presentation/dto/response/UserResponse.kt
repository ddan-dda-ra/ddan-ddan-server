package notbe.tmtm.ddanddanserver.presentation.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import notbe.tmtm.ddanddanserver.domain.model.User

@Schema(description = "User 응답 DTO")
data class UserResponse(
    @Schema(description = "User 식별 ID", example = "ABCDEF1234567")
    val id: String,
    @Schema(description = "User 표기 이름", example = "UserName")
    val name: String?,
    @Schema(description = "User 목표 칼로리", example = "100", minimum = "100", maximum = "1000")
    var purposeCalorie: Int,
    @Schema(description = "User 소유 먹이 개수", example = "10")
    var feed: Int,
) {
    companion object {
        fun fromDomain(user: User) =
            with(user) {
                UserResponse(
                    id = id,
                    name = name,
                    purposeCalorie = purposeCalorie,
                    feed = feed,
                )
            }
    }
}
