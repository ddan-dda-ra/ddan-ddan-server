package notbe.tmtm.ddanddanserver.application.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import notbe.tmtm.ddanddanserver.domain.exception.UserNotFoundException
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet

import notbe.tmtm.ddanddanserver.domain.model.user.DailyInfo
import notbe.tmtm.ddanddanserver.domain.model.user.DeviceToken
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.domain.model.user.UserSetting
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.DailyInfoRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.PetRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import org.bson.types.ObjectId
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.time.LocalDate
import java.util.Optional

class UserAdminServiceTest : FunSpec({
    lateinit var repository: UserRepository
    lateinit var dailyInfoRepository: DailyInfoRepository
    lateinit var petRepository: PetRepository
    lateinit var service: UserAdminService

    fun user(name: String?, mainPetId: ObjectId? = null): User =
        User(
            id = ObjectId(),
            deviceToken = DeviceToken("token"),
            name = name,
            mainPetId = mainPetId,
            setting = UserSetting(),
        )

    fun pet(type: String): Pet =
        Pet(
            id = ObjectId(),
            type = type,
            ownerUserId = ObjectId(),
            exp = 0,
        )

    fun dailyInfo(
        userId: ObjectId,
        date: LocalDate,
        calorie: Int,
    ): DailyInfo =
        DailyInfo(
            id = ObjectId(),
            userId = userId,
            userName = "테스터",
            petType = "CAT",
            calorie = calorie,
            purposeAchieved = calorie >= 100,
            toyGiven = false,
            date = date,
        )

    beforeEach {
        repository = mockk()
        dailyInfoRepository = mockk()
        petRepository = mockk()
        service = UserAdminService(repository, dailyInfoRepository, petRepository)
    }

    test("keyword가 null이면 findAll(pageable)을 호출한다") {
        val captured = slot<Pageable>()
        every { repository.findAll(capture(captured)) } returns PageImpl(listOf(user("ddingmin")))

        service.searchUsers(null, UserAdminSortType.LATEST_LOGIN, page = 0, size = 10)

        verify(exactly = 1) { repository.findAll(any<Pageable>()) }
        verify(exactly = 0) { repository.findByNameContainingIgnoreCase(any(), any()) }
    }

    test("keyword가 빈 문자열이거나 공백이면 findAll(pageable)을 호출한다") {
        every { repository.findAll(any<Pageable>()) } returns PageImpl(emptyList())

        service.searchUsers("   ", UserAdminSortType.LATEST_LOGIN, page = 0, size = 10)

        verify(exactly = 1) { repository.findAll(any<Pageable>()) }
    }

    test("keyword는 trim 후 사용된다") {
        val keywordSlot = slot<String>()
        every {
            repository.findByNameContainingIgnoreCase(capture(keywordSlot), any())
        } returns PageImpl(listOf(user("딴딴")))

        service.searchUsers("  딴딴  ", UserAdminSortType.LATEST_LOGIN, page = 0, size = 10)

        keywordSlot.captured shouldBe "딴딴"
    }

    test("keyword가 있으면 findByNameContainingIgnoreCase를 호출한다") {
        every { repository.findByNameContainingIgnoreCase("딴딴", any()) } returns PageImpl(listOf(user("딴딴")))

        service.searchUsers("딴딴", UserAdminSortType.LATEST_LOGIN, page = 0, size = 10)

        verify(exactly = 1) { repository.findByNameContainingIgnoreCase("딴딴", any()) }
    }

    test("LATEST_LOGIN sort는 lastLoginAt DESC로 강제된다") {
        val pageable = slot<Pageable>()
        every { repository.findAll(capture(pageable)) } returns PageImpl(emptyList())

        service.searchUsers(null, UserAdminSortType.LATEST_LOGIN, page = 0, size = 10)

        val orders = pageable.captured.sort.toList()
        orders.size shouldBe 1
        orders[0].property shouldBe "lastLoginAt"
        orders[0].isDescending shouldBe true
    }

    test("JOINED sort는 _id DESC로 강제된다") {
        val pageable = slot<Pageable>()
        every { repository.findAll(capture(pageable)) } returns PageImpl(emptyList())

        service.searchUsers(null, UserAdminSortType.JOINED, page = 0, size = 10)

        val orders = pageable.captured.sort.toList()
        orders[0].property shouldBe "_id"
        orders[0].isDescending shouldBe true
    }

    test("getUser는 ObjectId로 사용자를 조회한다") {
        val id = ObjectId()
        val expected = user("ddingmin")
        every { repository.findById(id) } returns Optional.of(expected)

        val result = service.getUser(id)

        result shouldBe expected
    }

    test("getUser가 존재하지 않으면 UserNotFoundException") {
        val id = ObjectId()
        every { repository.findById(id) } returns Optional.empty()

        shouldThrow<UserNotFoundException> {
            service.getUser(id)
        }
    }

    test("getDailyCalories는 유저 존재 확인 후 기간 내 daily_calories를 반환한다") {
        val id = ObjectId()
        val from = LocalDate.parse("2026-04-01")
        val to = LocalDate.parse("2026-04-30")
        every { repository.findById(id) } returns Optional.of(user("tester"))
        every {
            dailyInfoRepository.findAllByUserIdAndDateBetweenOrderByDateDesc(id, from, to)
        } returns
            listOf(
                dailyInfo(id, LocalDate.parse("2026-04-30"), 250),
                dailyInfo(id, LocalDate.parse("2026-04-29"), 80),
            )

        val result = service.getDailyCalories(id, from, to)

        result.size shouldBe 2
        result[0].calorie shouldBe 250
        result[0].purposeAchieved shouldBe true
        result[1].calorie shouldBe 80
        result[1].purposeAchieved shouldBe false
    }

    test("getDailyCalories는 유저가 존재하지 않으면 UserNotFoundException") {
        val id = ObjectId()
        every { repository.findById(id) } returns Optional.empty()

        shouldThrow<UserNotFoundException> {
            service.getDailyCalories(id, LocalDate.now().minusDays(7), LocalDate.now())
        }
    }

    test("getMainPetTypes는 mainPetId가 있는 유저들만 모아 한 번에 조회한다") {
        val cat = pet("CAT")
        val penguin = pet("PENGUIN")
        val u1 = user("a", mainPetId = cat.id)
        val u2 = user("b", mainPetId = null)
        val u3 = user("c", mainPetId = penguin.id)

        val ids = slot<Iterable<ObjectId>>()
        every { petRepository.findAllById(capture(ids)) } returns listOf(cat, penguin)

        val result = service.getMainPetTypes(listOf(u1, u2, u3))

        ids.captured.toList() shouldBe listOf(cat.id, penguin.id)
        result shouldBe mapOf(cat.id to "CAT", penguin.id to "PENGUIN")
    }

    test("getMainPetTypes는 모든 유저가 mainPetId 없으면 펫 repository를 호출하지 않는다") {
        val u = user("noPet", mainPetId = null)

        val result = service.getMainPetTypes(listOf(u))

        result shouldBe emptyMap()
        verify(exactly = 0) { petRepository.findAllById(any<Iterable<ObjectId>>()) }
    }

    test("getMainPetType는 user의 mainPetId가 null이면 null을 반환하고 repository를 호출하지 않는다") {
        val u = user("noPet", mainPetId = null)

        service.getMainPetType(u) shouldBe null

        verify(exactly = 0) { petRepository.findById(any()) }
    }

    test("getMainPetType는 mainPetId가 있으면 펫의 type을 반환한다") {
        val hamster = pet("HAMSTER")
        val u = user("hasPet", mainPetId = hamster.id)
        every { petRepository.findById(hamster.id) } returns Optional.of(hamster)

        service.getMainPetType(u) shouldBe "HAMSTER"
    }

    test("getMainPetType는 mainPetId가 있어도 펫이 미존재하면 null") {
        val u = user("stalePet", mainPetId = ObjectId())
        every { petRepository.findById(any()) } returns Optional.empty()

        service.getMainPetType(u) shouldBe null
    }
})
