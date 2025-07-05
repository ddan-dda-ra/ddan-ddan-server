package notbe.tmtm.ddanddanserver.presentation.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import notbe.tmtm.ddanddanserver.domain.model.friend.Friendship
import notbe.tmtm.ddanddanserver.domain.model.user.User
import java.time.LocalDateTime

@Schema(description = "친구 응답 DTO")
data class FriendshipResponse(
    @Schema(description = "친구 관계 ID", example = "507f1f77bcf86cd799439011")
    val id: String,
    @Schema(description = "친구 사용자 정보")
    val friendUser: FriendUserInfo,
    @Schema(description = "친구 관계 생성 시간", example = "2024-07-01T15:30:00")
    val createdAt: LocalDateTime,
    @Schema(description = "친구 관계 수락 시간", example = "2024-07-01T15:30:00")
    val acceptedAt: LocalDateTime?,
) {
    companion object {
        fun fromDomain(
            friendship: Friendship,
            friendUser: User,
        ): FriendshipResponse =
            FriendshipResponse(
                id = friendship.id.toString(),
                friendUser = FriendUserInfo.fromDomain(friendUser),
                createdAt = friendship.createdAt,
                acceptedAt = friendship.getAcceptedAt(),
            )
    }
}

@Schema(description = "친구 사용자 정보 DTO")
data class FriendUserInfo(
    @Schema(description = "사용자 ID", example = "507f1f77bcf86cd799439011")
    val id: String,
    @Schema(description = "사용자 이름", example = "홍길동")
    val name: String?,
) {
    companion object {
        fun fromDomain(user: User): FriendUserInfo =
            FriendUserInfo(
                id = user.id.toString(),
                name = user.name,
            )
    }
}

@Schema(description = "친구 목록 응답 DTO")
data class FriendListResponse(
    @Schema(description = "친구 목록")
    val friendships: List<FriendshipResponse>,
    @Schema(description = "총 친구 수", example = "5")
    val totalCount: Int,
) {
    companion object {
        fun fromDomain(friends: List<FriendshipResponse>): FriendListResponse =
            FriendListResponse(
                friendships = friends,
                totalCount = friends.size,
            )
    }
}
