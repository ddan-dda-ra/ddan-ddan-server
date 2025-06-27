package notbe.tmtm.ddanddanserver.application.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.pet.PetType
import notbe.tmtm.ddanddanserver.domain.model.user.DailyInfo
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.*
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UserPetServiceTest {
    private lateinit var userRepository: UserRepository
    private lateinit var petRepository: PetRepository
    private lateinit var dailyInfoRepository: DailyInfoRepository
    private lateinit var userPetService: UserPetService

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        petRepository = mockk()
        dailyInfoRepository = mockk()
        userPetService = UserPetService(userRepository, petRepository, dailyInfoRepository)
    }

    @Test
    fun `UserPetService가 정상적으로 생성되어야 한다`() {
        // when & then
        assertNotNull(userPetService)
    }

    @Test
    fun `메인 펫이 있는 경우 펫을 반환해야 한다`() {
        // given
        val userId = ObjectId()
        val petId = ObjectId()
        val user = mockk<User> {
            every { mainPetId } returns petId
        }
        val pet = mockk<Pet>()

        every { userRepository.findByIdOrThrow(userId) } returns user
        every { petRepository.findByIdAndOwnerUserIdOrThrow(petId, userId) } returns pet

        // when
        val result = userPetService.getMainPet(userId)

        // then
        assertNotNull(result)
        assertEquals(pet, result)
        verify { userRepository.findByIdOrThrow(userId) }
        verify { petRepository.findByIdAndOwnerUserIdOrThrow(petId, userId) }
    }

    @Test
    fun `메인 펫이 없는 경우 null을 반환해야 한다`() {
        // given
        val userId = ObjectId()
        val user = mockk<User> {
            every { mainPetId } returns null
        }

        every { userRepository.findByIdOrThrow(userId) } returns user

        // when
        val result = userPetService.getMainPet(userId)

        // then
        assertNull(result)
        verify { userRepository.findByIdOrThrow(userId) }
        verify(exactly = 0) { petRepository.findByIdAndOwnerUserIdOrThrow(any(), any()) }
    }

    @Test
    fun `메인 펫 설정이 성공해야 한다`() {
        // given
        val userId = ObjectId()
        val petId = ObjectId()
        val user = mockk<User>(relaxed = true) {
            every { id } returns userId
        }
        val pet = mockk<Pet> {
            every { id } returns petId
            every { type } returns PetType.DOG
        }
        val savedUser = mockk<User>()

        every { userRepository.findByIdOrThrow(userId) } returns user
        every { petRepository.findByIdAndOwnerUserIdOrThrow(petId, userId) } returns pet
        every { userRepository.save(user) } returns savedUser
        every { dailyInfoRepository.findByUserIdAndDate(userId, any()) } returns null

        // when
        val result = userPetService.setMainPet(userId, petId)

        // then
        verify { user.setMainPet(petId) }
        verify { userRepository.save(user) }
        assertEquals(savedUser, result.first)
        assertEquals(pet, result.second)
    }

    @Test
    fun `메인 펫 설정시 일일 정보가 있으면 펫 타입도 업데이트해야 한다`() {
        // given
        val userId = ObjectId()
        val petId = ObjectId()
        val user = mockk<User>(relaxed = true) {
            every { id } returns userId
        }
        val pet = mockk<Pet> {
            every { id } returns petId
            every { type } returns PetType.CAT
        }
        val dailyInfo = mockk<DailyInfo>(relaxed = true)
        val savedUser = mockk<User>()
        val savedDailyInfo = mockk<DailyInfo>()

        every { userRepository.findByIdOrThrow(userId) } returns user
        every { petRepository.findByIdAndOwnerUserIdOrThrow(petId, userId) } returns pet
        every { userRepository.save(user) } returns savedUser
        every { dailyInfoRepository.findByUserIdAndDate(userId, LocalDate.now()) } returns dailyInfo
        every { dailyInfoRepository.save(dailyInfo) } returns savedDailyInfo

        // when
        val result = userPetService.setMainPet(userId, petId)

        // then
        verify { user.setMainPet(petId) }
        verify { userRepository.save(user) }
        verify { dailyInfo.petType = PetType.CAT }
        verify { dailyInfoRepository.save(dailyInfo) }
        assertEquals(savedUser, result.first)
        assertEquals(pet, result.second)
    }

    @Test
    fun `메인 펫 설정시 일일 정보가 없으면 펫 타입 업데이트를 하지 않아야 한다`() {
        // given
        val userId = ObjectId()
        val petId = ObjectId()
        val user = mockk<User>(relaxed = true) {
            every { id } returns userId
        }
        val pet = mockk<Pet> {
            every { id } returns petId
            every { type } returns PetType.DOG
        }
        val savedUser = mockk<User>()

        every { userRepository.findByIdOrThrow(userId) } returns user
        every { petRepository.findByIdAndOwnerUserIdOrThrow(petId, userId) } returns pet
        every { userRepository.save(user) } returns savedUser
        every { dailyInfoRepository.findByUserIdAndDate(userId, LocalDate.now()) } returns null

        // when
        val result = userPetService.setMainPet(userId, petId)

        // then
        verify { user.setMainPet(petId) }
        verify { userRepository.save(user) }
        verify(exactly = 0) { dailyInfoRepository.save(any()) }
        assertEquals(savedUser, result.first)
        assertEquals(pet, result.second)
    }
}
