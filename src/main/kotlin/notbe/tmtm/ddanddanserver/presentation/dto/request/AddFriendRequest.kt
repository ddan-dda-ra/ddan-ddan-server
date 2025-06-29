package notbe.tmtm.ddanddanserver.presentation.dto.request

import io.swagger.v3.oas.annotations.media.Schema

data class AddFriendRequest(
    @Schema(description = "친구로 추가할 사용자 ID", example = "60d5f3e3c12a4b2f8c6d1234")
    val userId: String,
)
