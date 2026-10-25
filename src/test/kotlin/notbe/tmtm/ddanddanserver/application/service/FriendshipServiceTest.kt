package notbe.tmtm.ddanddanserver.application.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import notbe.tmtm.ddanddanserver.domain.model.friend.Friendship
import notbe.tmtm.ddanddanserver.domain.model.friend.InviteCode
import notbe.tmtm.ddanddanserver.domain.model.notification.RoutingView
import notbe.tmtm.ddanddanserver.domain.model.user.DeviceToken
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.domain.model.user.UserSetting
import notbe.tmtm.ddanddanserver.infrastructure.client.PushClient
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.FriendshipRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.InviteCodeRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.areFriends
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.findByIdOrThrow
import org.bson.types.ObjectId

class FriendshipServiceTest : FunSpec({
    lateinit var friendshipRepository: FriendshipRepository
    lateinit var inviteCodeRepository: InviteCodeRepository
    lateinit var userRepository: UserRepository
    lateinit var userPetService: UserPetService
    lateinit var pushClient: PushClient
    lateinit var friendshipService: FriendshipService

    beforeEach {
        friendshipRepository = mockk()
        inviteCodeRepository = mockk()
        userRepository = mockk()
        userPetService = mockk()
        pushClient = mockk(relaxed = true)
        friendshipService = FriendshipService(
            friendshipRepository,
            inviteCodeRepository,
            userRepository,
            userPetService,
            pushClient,
        )
    }

    fun setUpInvite(inviterPushOn: Boolean): Pair<InviteCode, ObjectId> {
        val inviterId = ObjectId()
        val inviteeId = ObjectId()
        val inviteCode = InviteCode.create(inviterId)
        val inviter = User(
            id = inviterId,
            deviceToken = DeviceToken("inviter-token"),
            name = "초대자",
            setting = UserSetting(isAppPushOn = inviterPushOn),
        )
        val invitee = User(
            id = inviteeId,
            deviceToken = DeviceToken("invitee-token"),
            name = "피초대자",
            setting = UserSetting(isAppPushOn = true),
        )

        every { inviteCodeRepository.findByCode(inviteCode.getCode()) } returns inviteCode
        every { userRepository.findByIdOrThrow(inviterId) } returns inviter
        every { userRepository.findByIdOrThrow(inviteeId) } returns invitee
        every { friendshipRepository.areFriends(inviterId, inviteeId) } returns false
        every { friendshipRepository.save(any<Friendship>()) } answers { firstArg() }

        return inviteCode to inviteeId
    }

    test("초대 코드로 친구를 추가하면 푸시가 켜진 초대자에게 알림을 보낸다") {
        val (inviteCode, inviteeId) = setUpInvite(inviterPushOn = true)

        val result = friendshipService.addFriendByInviteCode(inviteCode.getCode(), inviteeId)

        result.getInviteeId() shouldBe inviteeId
        verify(exactly = 1) {
            pushClient.sendToUser(DeviceToken("inviter-token"), "피초대자님과 친구가 되었습니다!", RoutingView.MAIN)
        }
    }

    test("초대자의 푸시 설정이 꺼져 있으면 친구 추가 알림을 보내지 않는다") {
        val (inviteCode, inviteeId) = setUpInvite(inviterPushOn = false)

        friendshipService.addFriendByInviteCode(inviteCode.getCode(), inviteeId)

        verify(exactly = 1) { friendshipRepository.save(any<Friendship>()) }
        verify(exactly = 0) { pushClient.sendToUser(any(), any(), any()) }
    }
})
