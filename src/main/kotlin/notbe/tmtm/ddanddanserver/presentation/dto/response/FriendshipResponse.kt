package notbe.tmtm.ddanddanserver.presentation.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import notbe.tmtm.ddanddanserver.domain.model.friend.Friendship
import notbe.tmtm.ddanddanserver.domain.model.user.UserMainPet
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
            friendUser: UserMainPet,
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
    @Schema(description = "주 펫 종류 키 (PetCatalog.key)", example = "DOG", nullable = true)
    val mainPetType: String?,
    @Schema(description = "펫 레벨", example = "1", nullable = true)
    val petLevel: Int?,
) {
    companion object {
        fun fromDomain(userMainPet: UserMainPet): FriendUserInfo =
            FriendUserInfo(
                id = userMainPet.user.id.toString(),
                name = userMainPet.user.name,
                mainPetType = userMainPet.mainPet?.type,
                petLevel = userMainPet.mainPet?.getLevel(),
            )
    }
}

@Schema(description = "초대자 정보 응답 DTO")
data class InviterResponse(
    @Schema(description = "초대자 사용자 정보")
    val inviterUser: FriendUserInfo,
) {
    companion object {
        fun fromDomain(userMainPet: UserMainPet): InviterResponse =
            InviterResponse(
                inviterUser = FriendUserInfo.fromDomain(userMainPet),
            )
    }
}

@Schema(description = "친구 목록 응답 DTO")
data class FriendListResponse(
    @Schema(description = "친구 목록")
    val friends: List<FriendUserInfo>,
    @Schema(description = "총 친구 수", example = "5")
    val totalCount: Int,
) {
    companion object {
        fun fromDomain(friends: List<UserMainPet>): FriendListResponse {
            val friendInfos = friends.map { FriendUserInfo.fromDomain(it) }

            return FriendListResponse(
                friends = friendInfos,
                totalCount = friends.size,
            )
        }
    }
}
