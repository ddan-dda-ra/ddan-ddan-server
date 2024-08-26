package notbe.tmtm.ddanddanserver.presentation.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "User 요청 DTO")
data class UserRequest(
    @Schema(description = "User 이름", example = "딴딴한 유저")
    val name: String,
    @Schema(description = "User 목표 칼로리", example = "100", minimum = "100", maximum = "1000")
    val purposeCalorie: Int,
)
