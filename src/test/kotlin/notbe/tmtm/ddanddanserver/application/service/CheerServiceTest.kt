package notbe.tmtm.ddanddanserver.application.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import notbe.tmtm.ddanddanserver.domain.exception.CheerAlreadyExistsException
import notbe.tmtm.ddanddanserver.domain.exception.CheerNotFriendsException
import notbe.tmtm.ddanddanserver.domain.model.cheer.Cheer
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
    lateinit var friendshipRepository: FriendshipRepository
    lateinit var userRepository: UserRepository
    lateinit var pushClient: PushClient
    lateinit var cheerService: CheerService

    beforeEach {
        cheerRepository = mockk()
        friendshipRepository = mockk()
        userRepository = mockk()
        pushClient = mockk(relaxed = true)
        cheerService = CheerService(cheerRepository, friendshipRepository, userRepository, pushClient)
    }

    test("친구 관계에서 응원을 생성할 수 있다") {
        val cheererId = ObjectId()
        val cheereeId = ObjectId()
        val expectedCheer = Cheer.create(cheererId, cheereeId)
        val cheereeUser = User(
            id = cheereeId,
            deviceToken = DeviceToken("test-token"),
            name = "테스트유저",
            setting = UserSetting(isAppPushOn = true)
        )

        every { userRepository.findByIdOrThrow(cheereeId) } returns cheereeUser
        every { friendshipRepository.areFriends(cheererId, cheereeId) } returns true
        every { cheerRepository.saveWithDuplicateCheck(any()) } returns expectedCheer

        val result = cheerService.createCheer(cheererId, cheereeId)

        result.cheererId shouldBe cheererId
        result.cheereeId shouldBe cheereeId
        verify { friendshipRepository.areFriends(cheererId, cheereeId) }
        verify { cheerRepository.saveWithDuplicateCheck(any()) }
        verify { pushClient.sendToUser(any(), any(), any()) }
    }

    test("친구가 아닌 사용자에게는 응원할 수 없다") {
        val cheererId = ObjectId()
        val cheereeId = ObjectId()
        val cheereeUser = User(
            id = cheereeId,
            deviceToken = DeviceToken("test-token"),
            name = "테스트유저",
            setting = UserSetting(isAppPushOn = true)
        )

        every { userRepository.findByIdOrThrow(cheereeId) } returns cheereeUser
        every { friendshipRepository.areFriends(cheererId, cheereeId) } returns false

        shouldThrow<CheerNotFriendsException> {
            cheerService.createCheer(cheererId, cheereeId)
        }

        verify { friendshipRepository.areFriends(cheererId, cheereeId) }
        verify(exactly = 0) { cheerRepository.saveWithDuplicateCheck(any()) }
    }

    test("자기 자신을 응원하려고 하면 예외가 발생한다") {
        val userId = ObjectId()
        val user = User(
            id = userId,
            deviceToken = DeviceToken("test-token"),
            name = "테스트유저",
            setting = UserSetting(isAppPushOn = true)
        )

        every { userRepository.findByIdOrThrow(userId) } returns user
        every { friendshipRepository.areFriends(userId, userId) } returns false

        shouldThrow<CheerNotFriendsException> {
            cheerService.createCheer(userId, userId)
        }
    }

    test("중복 응원 시도 시 예외가 발생한다") {
        val cheererId = ObjectId()
        val cheereeId = ObjectId()
        val cheereeUser = User(
            id = cheereeId,
            deviceToken = DeviceToken("test-token"),
            name = "테스트유저",
            setting = UserSetting(isAppPushOn = true)
        )

        every { userRepository.findByIdOrThrow(cheereeId) } returns cheereeUser
        every { friendshipRepository.areFriends(cheererId, cheereeId) } returns true
        every { cheerRepository.saveWithDuplicateCheck(any()) } throws CheerAlreadyExistsException()

        shouldThrow<CheerAlreadyExistsException> {
            cheerService.createCheer(cheererId, cheereeId)
        }

        verify { friendshipRepository.areFriends(cheererId, cheereeId) }
        verify { cheerRepository.saveWithDuplicateCheck(any()) }
    }

    test("월간 받은 응원 횟수를 조회할 수 있다") {
        val userId = ObjectId()
        val expectedCount = 15L

        every { cheerRepository.countMonthlyReceivedCheers(userId) } returns expectedCount

        val result = cheerService.getMonthlyReceivedCheerCount(userId)

        result shouldBe expectedCount
        verify { cheerRepository.countMonthlyReceivedCheers(userId) }
    }

    test("월간 받은 응원이 없으면 0을 반환한다") {
        val userId = ObjectId()

        every { cheerRepository.countMonthlyReceivedCheers(userId) } returns 0L

        val result = cheerService.getMonthlyReceivedCheerCount(userId)

        result shouldBe 0L
        verify { cheerRepository.countMonthlyReceivedCheers(userId) }
    }
})
