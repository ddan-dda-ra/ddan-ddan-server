package notbe.tmtm.ddanddanserver.presentation.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import notbe.tmtm.ddanddanserver.application.service.FriendService
import notbe.tmtm.ddanddanserver.domain.model.friend.FriendStatus
import java.time.LocalDateTime

data class FriendRequestResponse(
    @Schema(description = "친구 요청 ID")
    val id: String,
    @Schema(description = "요청자 사용자 정보")
    val requester: FriendUserInfo,
    @Schema(description = "요청 상태")
    val status: FriendStatus,
    @Schema(description = "요청 생성 시간")
    val createdAt: LocalDateTime,
) {
    companion object {
        fun fromDomain(requestWithUser: FriendService.FriendRequestWithUser): FriendRequestResponse {
            return FriendRequestResponse(
                id = requestWithUser.request.id.toString(),
                requester = FriendUserInfo.fromDomain(requestWithUser.requester),
                status = requestWithUser.request.getStatus(),
                createdAt = requestWithUser.request.createdAt,
            )
        }
    }
}

data class FriendRequestListResponse(
    @Schema(description = "받은 친구 요청 목록")
    val requests: List<FriendRequestResponse>,
    @Schema(description = "총 요청 수")
    val totalCount: Int,
) {
    companion object {
        fun fromDomain(requestsWithUser: List<FriendService.FriendRequestWithUser>): FriendRequestListResponse {
            return FriendRequestListResponse(
                requests = requestsWithUser.map { FriendRequestResponse.fromDomain(it) },
                totalCount = requestsWithUser.size,
            )
        }
    }
}
