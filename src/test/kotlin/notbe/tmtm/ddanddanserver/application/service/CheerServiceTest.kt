package notbe.tmtm.ddanddanserver.application.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import notbe.tmtm.ddanddanserver.domain.exception.CheerAlreadyExistsException
import notbe.tmtm.ddanddanserver.domain.exception.CheerNotFriendException
import notbe.tmtm.ddanddanserver.domain.exception.CheerSelfException
import notbe.tmtm.ddanddanserver.domain.exception.UserNotFoundException
import notbe.tmtm.ddanddanserver.domain.model.cheer.Cheer
import notbe.tmtm.ddanddanserver.domain.model.notification.RoutingView
import notbe.tmtm.ddanddanserver.domain.model.user.DeviceToken
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.domain.model.user.UserSetting
import notbe.tmtm.ddanddanserver.infrastructure.client.PushClient
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.CheerRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.FriendshipRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.areFriends
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.findByIdOrThrow
import org.bson.types.ObjectId

class CheerServiceTest : FunSpec({
    lateinit var cheerRepository: CheerRepository
    lateinit var userRepository: UserRepository
    lateinit var friendshipRepository: FriendshipRepository
    lateinit var pushClient: PushClient
    lateinit var cheerService: CheerService

    beforeEach {
        cheerRepository = mockk()
        userRepository = mockk()
        friendshipRepository = mockk()
        pushClient = mockk(relaxed = true)
        cheerService = CheerService(cheerRepository, userRepository, friendshipRepository, pushClient)
    }

    fun user(id: ObjectId, token: String, name: String) = User(
        id = id,
        deviceToken = DeviceToken(token),
        name = name,
        setting = UserSetting(isAppPushOn = true)
    )

    test("친구 관계인 유저를 응원할 수 있다") {
        val cheererId = ObjectId()
        val cheereeId = ObjectId()
        val expectedCheer = Cheer.create(cheererId, cheereeId)

        every { userRepository.findByIdOrThrow(cheereeId) } returns user(cheereeId, "test-token", "테스트유저")
        every { userRepository.findByIdOrThrow(cheererId) } returns user(cheererId, "cheerer-token", "응원자")
        every { friendshipRepository.areFriends(cheererId, cheereeId) } returns true
        every { cheerRepository.saveWithDuplicateCheck(any()) } returns expectedCheer

        val result = cheerService.createCheer(cheererId, cheereeId)

        result.cheererId shouldBe cheererId
        result.cheereeId shouldBe cheereeId
        verify { friendshipRepository.areFriends(cheererId, cheereeId) }
        verify { cheerRepository.saveWithDuplicateCheck(any()) }
        verify(exactly = 1) { pushClient.sendToUser(DeviceToken("test-token"), any(), RoutingView.MAIN) }
    }

    test("응원받는 유저의 푸시 설정이 꺼져 있으면 응원 알림을 보내지 않는다") {
        val cheererId = ObjectId()
        val cheereeId = ObjectId()
        val expectedCheer = Cheer.create(cheererId, cheereeId)
        val cheereeUser = User(
            id = cheereeId,
            deviceToken = DeviceToken("test-token"),
            name = "테스트유저",
            setting = UserSetting(isAppPushOn = false)
        )
        val cheererUser = User(
            id = cheererId,
            deviceToken = DeviceToken("cheerer-token"),
            name = "응원자",
            setting = UserSetting(isAppPushOn = true)
        )

        every { userRepository.findByIdOrThrow(cheereeId) } returns cheereeUser
        every { userRepository.findByIdOrThrow(cheererId) } returns cheererUser
        every { friendshipRepository.areFriends(cheererId, cheereeId) } returns true
        every { cheerRepository.saveWithDuplicateCheck(any()) } returns expectedCheer

        cheerService.createCheer(cheererId, cheereeId)

        verify(exactly = 1) { cheerRepository.saveWithDuplicateCheck(any()) }
        verify(exactly = 0) { pushClient.sendToUser(any(), any(), any()) }
    }

    test("친구 관계가 아닌 유저를 응원하면 예외가 발생하고 저장과 푸시를 하지 않는다") {
        val cheererId = ObjectId()
        val cheereeId = ObjectId()

        every { userRepository.findByIdOrThrow(cheereeId) } returns user(cheereeId, "test-token", "테스트유저")
        every { userRepository.findByIdOrThrow(cheererId) } returns user(cheererId, "cheerer-token", "응원자")
        every { friendshipRepository.areFriends(cheererId, cheereeId) } returns false

        shouldThrow<CheerNotFriendException> {
            cheerService.createCheer(cheererId, cheereeId)
        }

        verify(exactly = 0) { cheerRepository.saveWithDuplicateCheck(any()) }
        verify(exactly = 0) { pushClient.sendToUser(any(), any(), any()) }
    }

    test("자기 자신을 응원하면 친구 관계 확인 전에 자기 자신 응원 예외가 발생한다") {
        val userId = ObjectId()

        every { userRepository.findByIdOrThrow(userId) } returns user(userId, "test-token", "테스트유저")

        shouldThrow<CheerSelfException> {
            cheerService.createCheer(userId, userId)
        }

        verify(exactly = 0) { friendshipRepository.areFriends(any(), any()) }
        verify(exactly = 0) { cheerRepository.saveWithDuplicateCheck(any()) }
        verify(exactly = 0) { pushClient.sendToUser(any(), any(), any()) }
    }

    test("응원 대상 유저가 없으면 유저 미존재 예외가 발생한다") {
        val cheererId = ObjectId()
        val cheereeId = ObjectId()

        every { userRepository.findByIdOrThrow(cheereeId) } throws UserNotFoundException()

        shouldThrow<UserNotFoundException> {
            cheerService.createCheer(cheererId, cheereeId)
        }

        verify(exactly = 0) { friendshipRepository.areFriends(any(), any()) }
        verify(exactly = 0) { cheerRepository.saveWithDuplicateCheck(any()) }
    }

    test("중복 응원 시도 시 예외가 발생한다") {
        val cheererId = ObjectId()
        val cheereeId = ObjectId()

        every { userRepository.findByIdOrThrow(cheereeId) } returns user(cheereeId, "test-token", "테스트유저")
        every { userRepository.findByIdOrThrow(cheererId) } returns user(cheererId, "cheerer-token", "응원자")
        every { friendshipRepository.areFriends(cheererId, cheereeId) } returns true
        every { cheerRepository.saveWithDuplicateCheck(any()) } throws CheerAlreadyExistsException()

        shouldThrow<CheerAlreadyExistsException> {
            cheerService.createCheer(cheererId, cheereeId)
        }

        verify { cheerRepository.saveWithDuplicateCheck(any()) }
        verify(exactly = 0) { pushClient.sendToUser(any(), any(), any()) }
    }
})
