package notbe.tmtm.ddanddanserver.presentation.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import notbe.tmtm.ddanddanserver.application.service.FriendService
import notbe.tmtm.ddanddanserver.domain.model.friend.FriendStatus
import notbe.tmtm.ddanddanserver.domain.model.user.User
import java.time.LocalDateTime

data class FriendResponse(
    @Schema(description = "친구 관계 ID")
    val id: String,
    @Schema(description = "친구 사용자 정보")
    val user: FriendUserInfo,
    @Schema(description = "친구 관계 상태")
    val status: FriendStatus,
    @Schema(description = "친구 관계 생성 시간")
    val createdAt: LocalDateTime,
) {
    companion object {
        fun fromDomain(friendWithUser: FriendService.FriendWithUser): FriendResponse {
            return FriendResponse(
                id = friendWithUser.friendship.id.toString(),
                user = FriendUserInfo.fromDomain(friendWithUser.user),
                status = friendWithUser.friendship.getStatus(),
                createdAt = friendWithUser.friendship.createdAt,
            )
        }
    }
}

data class FriendUserInfo(
    @Schema(description = "사용자 ID")
    val id: String,
    @Schema(description = "사용자 이름")
    val name: String?,
    @Schema(description = "메인 펫 ID")
    val mainPetId: String?,
) {
    companion object {
        fun fromDomain(user: User): FriendUserInfo {
            return FriendUserInfo(
                id = user.id.toString(),
                name = user.name,
                mainPetId = user.mainPetId?.toString(),
            )
        }
    }
}
