package notbe.tmtm.ddanddanserver.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import notbe.tmtm.ddanddanserver.application.service.FriendService
import notbe.tmtm.ddanddanserver.presentation.dto.request.AddFriendRequest
import notbe.tmtm.ddanddanserver.presentation.dto.response.FriendListResponse
import notbe.tmtm.ddanddanserver.presentation.dto.response.FriendRequestListResponse
import org.bson.types.ObjectId
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/friends")
@Tag(name = "친구")
class FriendController(
    private val friendService: FriendService,
) {

    @PostMapping
    @Operation(summary = "친구 추가", description = "다른 사용자에게 친구 신청을 보냅니다.")
    fun sendFriendRequest(
        authentication: Authentication,
        @RequestBody
        request: AddFriendRequest,
    ): ResponseEntity<Void> {
        val userId = ObjectId(authentication.name)
        val targetUserId = ObjectId(request.userId)

        friendService.sendFriendRequest(userId, targetUserId)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/by-invite/{userId}")
    @Operation(summary = "딥링크로 친구 추가", description = "딥링크를 통해 특정 사용자에게 친구 신청을 보냅니다.")
    fun sendFriendRequestByInvite(
        authentication: Authentication,
        @PathVariable
        userId: String,
    ): ResponseEntity<Void> {
        val requesterId = ObjectId(authentication.name)
        val receiverId = ObjectId(userId)

        friendService.sendFriendRequest(requesterId, receiverId)
        return ResponseEntity.ok().build()
    }

    @GetMapping
    @Operation(summary = "친구 목록 조회", description = "내 친구 목록을 조회합니다.")
    fun getFriends(authentication: Authentication): FriendListResponse {
        val userId = ObjectId(authentication.name)
        val friends = friendService.getFriends(userId)
        return FriendListResponse.fromDomain(friends)
    }

    @GetMapping("/requests")
    @Operation(summary = "받은 친구 요청 조회", description = "받은 친구 요청 목록을 조회합니다.")
    fun getPendingRequests(authentication: Authentication): FriendRequestListResponse {
        val userId = ObjectId(authentication.name)
        val requests = friendService.getPendingRequestsReceived(userId)
        return FriendRequestListResponse.fromDomain(requests)
    }

    @PutMapping("/requests/{requestId}/accept")
    @Operation(summary = "친구 요청 수락", description = "받은 친구 요청를 수락합니다.")
    fun acceptFriendRequest(
        authentication: Authentication,
        @PathVariable
        requestId: String,
    ): ResponseEntity<Void> {
        val userId = ObjectId(authentication.name)
        val friendRequestId = ObjectId(requestId)

        friendService.acceptFriendRequest(friendRequestId, userId)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/requests/{requestId}/reject")
    @Operation(summary = "친구 요청 거부", description = "받은 친구 요청를 거부합니다.")
    fun rejectFriendRequest(
        authentication: Authentication,
        @PathVariable
        requestId: String,
    ): ResponseEntity<Void> {
        val userId = ObjectId(authentication.name)
        val friendRequestId = ObjectId(requestId)

        friendService.rejectFriendRequest(friendRequestId, userId)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{friendId}")
    @Operation(summary = "친구 삭제", description = "친구 관계를 삭제합니다.")
    fun removeFriend(
        authentication: Authentication,
        @PathVariable
        friendId: String,
    ): ResponseEntity<Void> {
        val userId = ObjectId(authentication.name)
        val targetUserId = ObjectId(friendId)

        friendService.removeFriend(userId, targetUserId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/invite-link")
    @Operation(summary = "내 초대 링크 생성", description = "다른 사용자가 나에게 친구 신청을 보낼 수 있는 딥링크를 생성합니다.")
    fun generateInviteLink(authentication: Authentication): Map<String, String> {
        val userId = authentication.name
        val inviteLink = "myapp://invite?userId=$userId"

        return mapOf("inviteLink" to inviteLink)
    }
}
