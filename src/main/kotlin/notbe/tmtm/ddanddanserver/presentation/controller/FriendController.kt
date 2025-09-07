package notbe.tmtm.ddanddanserver.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import notbe.tmtm.ddanddanserver.application.service.FriendshipService
import notbe.tmtm.ddanddanserver.application.service.InviteCodeService
import notbe.tmtm.ddanddanserver.application.service.UserPetService
import notbe.tmtm.ddanddanserver.presentation.dto.response.FriendListResponse
import notbe.tmtm.ddanddanserver.presentation.dto.response.FriendshipResponse
import notbe.tmtm.ddanddanserver.presentation.dto.response.InviteCodeResponse
import org.bson.types.ObjectId
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/friends")
@Tag(name = "친구")
class FriendController(
    private val friendshipService: FriendshipService,
    private val inviteCodeService: InviteCodeService,
    private val userPetService: UserPetService,
) {
    @PostMapping("/invite-codes")
    @Operation(summary = "초대코드 생성", description = "새로운 초대코드를 생성합니다.")
    fun generateInviteCode(authentication: Authentication): InviteCodeResponse {
        val userId = ObjectId(authentication.name)

        val inviteCode = inviteCodeService.generateInviteCode(userId)
        return InviteCodeResponse.fromDomain(inviteCode)
    }

    @PostMapping("/by-invite/{code}")
    @Operation(summary = "초대코드로 친구 추가", description = "초대코드를 사용하여 친구를 추가합니다.")
    fun addFriendByInviteCode(
        @PathVariable code: String,
        authentication: Authentication,
    ): FriendshipResponse {
        val userId = ObjectId(authentication.name)

        val friend = friendshipService.addFriendByInviteCode(code, userId)
        val inviterUser = userPetService.getUserMainPet(friend.getInviterId())

        return FriendshipResponse.fromDomain(friend, inviterUser)
    }

    @GetMapping("/me")
    @Operation(summary = "내 친구 목록 조회", description = "내 친구 목록을 조회합니다.")
    fun getMyFriends(authentication: Authentication): FriendListResponse {
        val userId = ObjectId(authentication.name)

        val friendIds = friendshipService.getFriendIds(userId)
        val friendMainPets = userPetService.getUserMainPets(friendIds)

        return FriendListResponse.fromDomain(friendMainPets)
    }

    @DeleteMapping("/{friendId}")
    @Operation(summary = "친구 삭제", description = "친구 관계를 삭제합니다.")
    fun removeFriend(
        @PathVariable friendId: String,
        authentication: Authentication,
    ) {
        val userId = ObjectId(authentication.name)
        val friendObjectId = ObjectId(friendId)

        friendshipService.removeFriend(friendObjectId, userId)
    }
}
