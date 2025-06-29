package notbe.tmtm.ddanddanserver.presentation.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import notbe.tmtm.ddanddanserver.application.service.FriendService

data class FriendListResponse(
    @Schema(description = "친구 목록")
    val friends: List<FriendResponse>,
    @Schema(description = "총 친구 수")
    val totalCount: Int,
) {
    companion object {
        fun fromDomain(friendsWithUser: List<FriendService.FriendWithUser>): FriendListResponse {
            return FriendListResponse(
                friends = friendsWithUser.map { FriendResponse.fromDomain(it) },
                totalCount = friendsWithUser.size,
            )
        }
    }
}
