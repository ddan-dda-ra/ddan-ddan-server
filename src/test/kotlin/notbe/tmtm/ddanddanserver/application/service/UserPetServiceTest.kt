package notbe.tmtm.ddanddanserver.application.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet

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
    fun `메인 펫 설정은 main_pet_id와 오늘 일일 정보의 펫 타입만 갱신해야 한다`() {
        // given
        val userId = ObjectId()
        val petId = ObjectId()
        val user = mockk<User> {
            every { id } returns userId
        }
        val pet = mockk<Pet> {
            every { id } returns petId
            every { type } returns "CAT"
        }
        val savedUser = mockk<User>()

        every { userRepository.findByIdOrThrow(userId) } returns user
        every { petRepository.findByIdAndOwnerUserIdOrThrow(petId, userId) } returns pet
        every { userRepository.updateMainPetId(userId, petId) } returns savedUser
        every { dailyInfoRepository.updatePetType(userId, LocalDate.now(), "CAT") } returns Unit

        // when
        val result = userPetService.setMainPet(userId, petId)

        // then
        verify { userRepository.updateMainPetId(userId, petId) }
        verify { dailyInfoRepository.updatePetType(userId, LocalDate.now(), "CAT") }
        verify(exactly = 0) { userRepository.save(any()) }
        verify(exactly = 0) { dailyInfoRepository.save(any()) }
        assertEquals(savedUser, result.first)
        assertEquals(pet, result.second)
    }
}
