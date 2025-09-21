package notbe.tmtm.ddanddanserver.application.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import notbe.tmtm.ddanddanserver.domain.model.cheer.Cheer
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.pet.PetType
import notbe.tmtm.ddanddanserver.domain.model.user.DailyInfo
import notbe.tmtm.ddanddanserver.domain.model.user.DeviceToken
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.domain.model.user.UserSetting
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.CheerRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.DailyInfoRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.FriendshipRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.PetRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.areFriends
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.findByIdAndOwnerUserIdOrThrow
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.findByIdOrThrow
import org.bson.types.ObjectId
import java.time.LocalDate

class UserProfileServiceTest : FunSpec({
    lateinit var userRepository: UserRepository
    lateinit var dailyInfoRepository: DailyInfoRepository
    lateinit var petRepository: PetRepository
    lateinit var cheerRepository: CheerRepository
    lateinit var friendshipRepository: FriendshipRepository
    lateinit var userProfileService: UserProfileService

    beforeEach {
        userRepository = mockk()
        dailyInfoRepository = mockk()
        petRepository = mockk()
        cheerRepository = mockk()
        friendshipRepository = mockk()
        userProfileService = UserProfileService(
            userRepository,
            dailyInfoRepository,
            petRepository,
            cheerRepository,
            friendshipRepository
        )
    }

    test("사용자 프로필을 조회할 수 있다") {
        val userId = ObjectId()
        val myId = ObjectId()
        val mainPetId = ObjectId()
        val today = LocalDate.now()

        val user = User(
            id = userId,
            deviceToken = DeviceToken("test-token"),
            name = "테스트유저",
            setting = UserSetting(isAppPushOn = true),
            mainPetId = mainPetId
        )

        val mainPet = Pet(
            id = mainPetId,
            type = PetType.CAT,
            ownerUserId = userId,
            exp = 100
        )

        val dailyInfo = DailyInfo.create(
            userId = userId,
            userName = "테스트유저",
            petType = PetType.CAT,
            date = today
        )

        val cheers = listOf(
            Cheer.create(myId, userId)
        )

        every { userRepository.findByIdOrThrow(userId) } returns user
        every { petRepository.findByIdAndOwnerUserIdOrThrow(mainPetId, userId) } returns mainPet
        every { dailyInfoRepository.findByUserIdAndDate(userId, today) } returns dailyInfo
        every { cheerRepository.findAllByCheereeIdAndThisMonth(userId) } returns cheers
        every { friendshipRepository.areFriends(userId, myId) } returns true

        val result = userProfileService.getUserProfile(userId, myId)

        result.user shouldBe user
        result.mainPet shouldBe mainPet
        result.todayDailyInfo shouldBe dailyInfo
        result.receivedCheers shouldBe cheers
        result.isFriend shouldBe true

        verify { userRepository.findByIdOrThrow(userId) }
        verify { petRepository.findByIdAndOwnerUserIdOrThrow(mainPetId, userId) }
        verify { dailyInfoRepository.findByUserIdAndDate(userId, today) }
        verify { cheerRepository.findAllByCheereeIdAndThisMonth(userId) }
        verify { friendshipRepository.areFriends(userId, myId) }
    }

    test("오늘의 DailyInfo가 없으면 기본값을 생성한다") {
        val userId = ObjectId()
        val myId = ObjectId()
        val mainPetId = ObjectId()
        val today = LocalDate.now()

        val user = User(
            id = userId,
            deviceToken = DeviceToken("test-token"),
            name = "테스트유저",
            setting = UserSetting(isAppPushOn = true),
            mainPetId = mainPetId
        )

        val mainPet = Pet(
            id = mainPetId,
            type = PetType.DOG,
            ownerUserId = userId,
            exp = 200
        )

        val cheers = emptyList<Cheer>()

        every { userRepository.findByIdOrThrow(userId) } returns user
        every { petRepository.findByIdAndOwnerUserIdOrThrow(mainPetId, userId) } returns mainPet
        every { dailyInfoRepository.findByUserIdAndDate(userId, today) } returns null
        every { cheerRepository.findAllByCheereeIdAndThisMonth(userId) } returns cheers
        every { friendshipRepository.areFriends(userId, myId) } returns false

        val result = userProfileService.getUserProfile(userId, myId)

        result.user shouldBe user
        result.mainPet shouldBe mainPet
        result.todayDailyInfo.userId shouldBe userId
        result.todayDailyInfo.userName shouldBe "테스트유저"
        result.todayDailyInfo.petType shouldBe PetType.DOG
        result.todayDailyInfo.date shouldBe today
        result.todayDailyInfo.calorie shouldBe 0
        result.receivedCheers shouldBe cheers
        result.isFriend shouldBe false

        verify { userRepository.findByIdOrThrow(userId) }
        verify { petRepository.findByIdAndOwnerUserIdOrThrow(mainPetId, userId) }
        verify { dailyInfoRepository.findByUserIdAndDate(userId, today) }
        verify { cheerRepository.findAllByCheereeIdAndThisMonth(userId) }
        verify { friendshipRepository.areFriends(userId, myId) }
    }

    test("받은 응원 목록이 비어있을 수 있다") {
        val userId = ObjectId()
        val myId = ObjectId()
        val mainPetId = ObjectId()
        val today = LocalDate.now()

        val user = User(
            id = userId,
            deviceToken = DeviceToken("test-token"),
            name = "테스트유저",
            setting = UserSetting(isAppPushOn = true),
            mainPetId = mainPetId
        )

        val mainPet = Pet(
            id = mainPetId,
            type = PetType.MOLE,
            ownerUserId = userId,
            exp = 50
        )

        val dailyInfo = DailyInfo.create(
            userId = userId,
            userName = "테스트유저",
            petType = PetType.MOLE,
            date = today
        )

        every { userRepository.findByIdOrThrow(userId) } returns user
        every { petRepository.findByIdAndOwnerUserIdOrThrow(mainPetId, userId) } returns mainPet
        every { dailyInfoRepository.findByUserIdAndDate(userId, today) } returns dailyInfo
        every { cheerRepository.findAllByCheereeIdAndThisMonth(userId) } returns emptyList()
        every { friendshipRepository.areFriends(userId, myId) } returns true

        val result = userProfileService.getUserProfile(userId, myId)

        result.user shouldBe user
        result.mainPet shouldBe mainPet
        result.todayDailyInfo shouldBe dailyInfo
        result.receivedCheers shouldBe emptyList()
        result.isFriend shouldBe true

        verify { userRepository.findByIdOrThrow(userId) }
        verify { petRepository.findByIdAndOwnerUserIdOrThrow(mainPetId, userId) }
        verify { dailyInfoRepository.findByUserIdAndDate(userId, today) }
        verify { cheerRepository.findAllByCheereeIdAndThisMonth(userId) }
        verify { friendshipRepository.areFriends(userId, myId) }
    }
})
