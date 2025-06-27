package notbe.tmtm.ddanddanserver.application.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import notbe.tmtm.ddanddanserver.domain.exception.PetMaxLevelException
import notbe.tmtm.ddanddanserver.domain.exception.PetNotFoundException
import notbe.tmtm.ddanddanserver.domain.exception.UserNotFoundException
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.pet.PetType
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.PetRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.findByIdAndOwnerUserIdOrThrow
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.findByIdOrThrow
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class PetServiceTest {
    private lateinit var petRepository: PetRepository
    private lateinit var userRepository: UserRepository
    private lateinit var petService: PetService

    @BeforeEach
    fun setUp() {
        petRepository = mockk()
        userRepository = mockk()
        petService = PetService(petRepository, userRepository)
    }

    @Test
    fun `펫을 추가해야 한다`() {
        // given
        val ownerUserId = ObjectId()
        val petType = PetType.DOG
        val savedPet = mockk<Pet>()

        every { petRepository.save(any()) } returns savedPet

        // when
        val result = petService.addPet(ownerUserId, petType)

        // then
        verify { petRepository.save(any()) }
        assertEquals(savedPet, result)
    }

    @Test
    fun `랜덤 펫을 추가해야 한다`() {
        // given
        val ownerUserId = ObjectId()
        val existingPet = mockk<Pet>()
        val savedPet = mockk<Pet>()

        every { existingPet.type } returns PetType.DOG
        every { petRepository.findAllByOwnerUserId(ownerUserId) } returns listOf(existingPet)
        every { petRepository.save(any()) } returns savedPet

        // when
        val result = petService.addRandomPet(ownerUserId)

        // then
        verify { petRepository.findAllByOwnerUserId(ownerUserId) }
        verify { petRepository.save(any()) }
        assertEquals(savedPet, result)
    }

    @Test
    fun `펫에게 먹이를 주어야 한다`() {
        // given
        val ownerUserId = ObjectId()
        val petId = ObjectId()
        val user = mockk<User>(relaxed = true)
        val pet = mockk<Pet>(relaxed = true)
        val savedUser = mockk<User>()
        val savedPet = mockk<Pet>()

        every { userRepository.findByIdOrThrow(ownerUserId) } returns user
        every { petRepository.findByIdAndOwnerUserIdOrThrow(petId, ownerUserId) } returns pet
        every { pet.isMaxLevel() } returns false
        every { userRepository.save(user) } returns savedUser
        every { petRepository.save(pet) } returns savedPet

        // when
        val result = petService.feedPet(ownerUserId, petId)

        // then
        verify { pet.eat() }
        verify { user.feed() }
        verify { userRepository.save(user) }
        verify { petRepository.save(pet) }
        assertEquals(savedUser, result.user)
        assertEquals(savedPet, result.pet)
    }

    @Test
    fun `펫과 놀아주어야 한다`() {
        // given
        val ownerUserId = ObjectId()
        val petId = ObjectId()
        val user = mockk<User>(relaxed = true)
        val pet = mockk<Pet>(relaxed = true)
        val savedUser = mockk<User>()
        val savedPet = mockk<Pet>()

        every { userRepository.findByIdOrThrow(ownerUserId) } returns user
        every { petRepository.findByIdAndOwnerUserIdOrThrow(petId, ownerUserId) } returns pet
        every { pet.isMaxLevel() } returns false
        every { userRepository.save(user) } returns savedUser
        every { petRepository.save(pet) } returns savedPet

        // when
        val result = petService.playPet(ownerUserId, petId)

        // then
        verify { pet.play() }
        verify { user.play() }
        verify { userRepository.save(user) }
        verify { petRepository.save(pet) }
        assertEquals(savedUser, result.user)
        assertEquals(savedPet, result.pet)
    }

    @Test
    fun `펫을 조회해야 한다`() {
        // given
        val userId = ObjectId()
        val petId = ObjectId()
        val pet = mockk<Pet>()

        every { petRepository.findByIdAndOwnerUserIdOrThrow(petId, userId) } returns pet

        // when
        val result = petService.getMyPet(userId, petId)

        // then
        verify { petRepository.findByIdAndOwnerUserIdOrThrow(petId, userId) }
        assertEquals(pet, result)
    }

    @Test
    fun `소유자의 모든 펫을 조회해야 한다`() {
        // given
        val ownerUserId = ObjectId()
        val pets = listOf(mockk<Pet>(), mockk<Pet>())

        every { petRepository.findAllByOwnerUserId(ownerUserId) } returns pets

        // when
        val result = petService.getPetsByOwner(ownerUserId)

        // then
        verify { petRepository.findAllByOwnerUserId(ownerUserId) }
        assertEquals(pets, result)
    }

    @Test
    fun `존재하지 않는 사용자가 펫을 먹이려 하면 예외가 발생해야 한다`() {
        // given
        val ownerUserId = ObjectId()
        val petId = ObjectId()

        every { userRepository.findByIdOrThrow(ownerUserId) } throws UserNotFoundException("User not found")

        // when & then
        assertThrows<UserNotFoundException> {
            petService.feedPet(ownerUserId, petId)
        }
    }

    @Test
    fun `존재하지 않는 펫을 먹이려 하면 예외가 발생해야 한다`() {
        // given
        val ownerUserId = ObjectId()
        val petId = ObjectId()
        val user = mockk<User>()

        every { userRepository.findByIdOrThrow(ownerUserId) } returns user
        every { petRepository.findByIdAndOwnerUserIdOrThrow(petId, ownerUserId) } throws PetNotFoundException("Pet not found")

        // when & then
        assertThrows<PetNotFoundException> {
            petService.feedPet(ownerUserId, petId)
        }
    }

    @Test
    fun `펫의 소유자가 아닌 사용자가 먹이를 주려 하면 예외가 발생해야 한다`() {
        // given
        val ownerUserId = ObjectId()
        val petId = ObjectId()
        val user = mockk<User>()

        every { userRepository.findByIdOrThrow(ownerUserId) } returns user
        every { petRepository.findByIdAndOwnerUserIdOrThrow(petId, ownerUserId) } throws PetNotFoundException("Pet not found")

        // when & then
        assertThrows<PetNotFoundException> {
            petService.feedPet(ownerUserId, petId)
        }
    }

    @Test
    fun `최대 레벨 펫에게 먹이를 주려 하면 예외가 발생해야 한다`() {
        // given
        val ownerUserId = ObjectId()
        val petId = ObjectId()
        val user = mockk<User>()
        val pet = mockk<Pet>()

        every { userRepository.findByIdOrThrow(ownerUserId) } returns user
        every { petRepository.findByIdAndOwnerUserIdOrThrow(petId, ownerUserId) } returns pet
        every { pet.isMaxLevel() } returns true

        // when & then
        assertThrows<PetMaxLevelException> {
            petService.feedPet(ownerUserId, petId)
        }
    }

    @Test
    fun `펫의 소유자가 아닌 사용자가 놀아주려 하면 예외가 발생해야 한다`() {
        // given
        val ownerUserId = ObjectId()
        val petId = ObjectId()
        val user = mockk<User>()

        every { userRepository.findByIdOrThrow(ownerUserId) } returns user
        every { petRepository.findByIdAndOwnerUserIdOrThrow(petId, ownerUserId) } throws PetNotFoundException("Pet not found")

        // when & then
        assertThrows<PetNotFoundException> {
            petService.playPet(ownerUserId, petId)
        }
    }

    @Test
    fun `최대 레벨 펫과 놀아주려 하면 예외가 발생해야 한다`() {
        // given
        val ownerUserId = ObjectId()
        val petId = ObjectId()
        val user = mockk<User>()
        val pet = mockk<Pet>()

        every { userRepository.findByIdOrThrow(ownerUserId) } returns user
        every { petRepository.findByIdAndOwnerUserIdOrThrow(petId, ownerUserId) } returns pet
        every { pet.isMaxLevel() } returns true

        // when & then
        assertThrows<PetMaxLevelException> {
            petService.playPet(ownerUserId, petId)
        }
    }

    @Test
    fun `펫의 소유자가 아닌 사용자가 펫을 조회하려 하면 예외가 발생해야 한다`() {
        // given
        val userId = ObjectId()
        val petId = ObjectId()

        every { petRepository.findByIdAndOwnerUserIdOrThrow(petId, userId) } throws PetNotFoundException("Pet not found")

        // when & then
        assertThrows<PetNotFoundException> {
            petService.getMyPet(userId, petId)
        }
    }

    @Test
    fun `존재하지 않는 펫을 조회하려 하면 예외가 발생해야 한다`() {
        // given
        val userId = ObjectId()
        val petId = ObjectId()

        every { petRepository.findByIdAndOwnerUserIdOrThrow(petId, userId) } throws PetNotFoundException("Pet not found")

        // when & then
        assertThrows<PetNotFoundException> {
            petService.getMyPet(userId, petId)
        }
    }
}
