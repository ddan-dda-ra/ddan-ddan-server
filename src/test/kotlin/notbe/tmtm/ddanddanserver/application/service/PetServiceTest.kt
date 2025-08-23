package notbe.tmtm.ddanddanserver.application.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import notbe.tmtm.ddanddanserver.domain.exception.PetNotFoundException
import notbe.tmtm.ddanddanserver.domain.exception.UserNotFoundException
import notbe.tmtm.ddanddanserver.domain.exception.UserTicketLackException
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.pet.PetType
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.PetRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.findByIdAndOwnerUserIdOrThrow
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.findByIdOrThrow
import org.bson.types.ObjectId

class PetServiceTest : FunSpec({
    lateinit var petRepository: PetRepository
    lateinit var userRepository: UserRepository
    lateinit var petService: PetService

    beforeEach {
        petRepository = mockk()
        userRepository = mockk()
        petService = PetService(petRepository, userRepository)
    }

    test("펫 추가: 펫을 성공적으로 추가해야 한다") {
        val ownerUserId = ObjectId()
        val petType = PetType.DOG
        val savedPet = mockk<Pet>()

        every { petRepository.save(any()) } returns savedPet

        val result = petService.addPet(ownerUserId, petType)

        verify { petRepository.save(any()) }
        result shouldBe savedPet
    }

    test("랜덤 펫 추가: 랜덤 펫을 성공적으로 추가해야 한다") {
        val ownerUserId = ObjectId()
        val existingPet = mockk<Pet>()
        val savedPet = mockk<Pet>()

        every { existingPet.type } returns PetType.DOG
        every { petRepository.findAllByOwnerUserId(ownerUserId) } returns listOf(existingPet)
        every { petRepository.save(any()) } returns savedPet

        val result = petService.addRandomPet(ownerUserId)

        verify { petRepository.findAllByOwnerUserId(ownerUserId) }
        verify { petRepository.save(any()) }
        result shouldBe savedPet
    }

    test("펫 먹이주기: 펫에게 먹이를 성공적으로 주어야 한다") {
        val ownerUserId = ObjectId()
        val petId = ObjectId()
        val user = mockk<User>(relaxed = true)
        val pet = mockk<Pet>(relaxed = true)
        val savedUser = mockk<User>()
        val savedPet = mockk<Pet>()

        every { userRepository.findByIdOrThrow(ownerUserId) } returns user
        every { petRepository.findByIdAndOwnerUserIdOrThrow(petId, ownerUserId) } returns pet
        every { userRepository.save(user) } returns savedUser
        every { petRepository.save(pet) } returns savedPet

        val result = petService.feedPet(ownerUserId, petId)

        verify { pet.eat() }
        verify { user.feed() }
        verify { userRepository.save(user) }
        verify { petRepository.save(pet) }
        result.user shouldBe savedUser
        result.pet shouldBe savedPet
    }

    test("펫 놀아주기: 펫과 성공적으로 놀아주어야 한다") {
        val ownerUserId = ObjectId()
        val petId = ObjectId()
        val user = mockk<User>(relaxed = true)
        val pet = mockk<Pet>(relaxed = true)
        val savedUser = mockk<User>()
        val savedPet = mockk<Pet>()

        every { userRepository.findByIdOrThrow(ownerUserId) } returns user
        every { petRepository.findByIdAndOwnerUserIdOrThrow(petId, ownerUserId) } returns pet
        every { userRepository.save(user) } returns savedUser
        every { petRepository.save(pet) } returns savedPet

        val result = petService.playPet(ownerUserId, petId)

        verify { pet.play() }
        verify { user.play() }
        verify { userRepository.save(user) }
        verify { petRepository.save(pet) }
        result.user shouldBe savedUser
        result.pet shouldBe savedPet
    }

    test("펫 조회: 내 펫을 성공적으로 조회해야 한다") {
        val userId = ObjectId()
        val petId = ObjectId()
        val pet = mockk<Pet>()

        every { petRepository.findByIdAndOwnerUserIdOrThrow(petId, userId) } returns pet

        val result = petService.getMyPet(userId, petId)

        verify { petRepository.findByIdAndOwnerUserIdOrThrow(petId, userId) }
        result shouldBe pet
    }

    test("펫 목록 조회: 소유자의 모든 펫을 성공적으로 조회해야 한다") {
        val ownerUserId = ObjectId()
        val pets = listOf(mockk<Pet>(), mockk<Pet>())

        every { petRepository.findAllByOwnerUserId(ownerUserId) } returns pets

        val result = petService.getPetsByOwner(ownerUserId)

        verify { petRepository.findAllByOwnerUserId(ownerUserId) }
        result shouldBe pets
    }

    test("예외 처리: 존재하지 않는 사용자가 펫을 먹이려 하면 예외가 발생해야 한다") {
        val ownerUserId = ObjectId()
        val petId = ObjectId()

        every { userRepository.findByIdOrThrow(ownerUserId) } throws UserNotFoundException("User not found")

        shouldThrow<UserNotFoundException> {
            petService.feedPet(ownerUserId, petId)
        }
    }

    test("예외 처리: 존재하지 않는 펫을 먹이려 하면 예외가 발생해야 한다") {
        val ownerUserId = ObjectId()
        val petId = ObjectId()
        val user = mockk<User>()

        every { userRepository.findByIdOrThrow(ownerUserId) } returns user
        every {
            petRepository.findByIdAndOwnerUserIdOrThrow(
                petId,
                ownerUserId
            )
        } throws PetNotFoundException("Pet not found")

        shouldThrow<PetNotFoundException> {
            petService.feedPet(ownerUserId, petId)
        }
    }

    test("예외 처리: 펫의 소유자가 아닌 사용자가 먹이를 주려 하면 예외가 발생해야 한다") {
        val ownerUserId = ObjectId()
        val petId = ObjectId()
        val user = mockk<User>()

        every { userRepository.findByIdOrThrow(ownerUserId) } returns user
        every {
            petRepository.findByIdAndOwnerUserIdOrThrow(
                petId,
                ownerUserId
            )
        } throws PetNotFoundException("Pet not found")

        shouldThrow<PetNotFoundException> {
            petService.feedPet(ownerUserId, petId)
        }
    }

    test("예외 처리: 펫의 소유자가 아닌 사용자가 놀아주려 하면 예외가 발생해야 한다") {
        val ownerUserId = ObjectId()
        val petId = ObjectId()
        val user = mockk<User>()

        every { userRepository.findByIdOrThrow(ownerUserId) } returns user
        every {
            petRepository.findByIdAndOwnerUserIdOrThrow(
                petId,
                ownerUserId
            )
        } throws PetNotFoundException("Pet not found")

        shouldThrow<PetNotFoundException> {
            petService.playPet(ownerUserId, petId)
        }
    }

    test("예외 처리: 펫의 소유자가 아닌 사용자가 펫을 조회하려 하면 예외가 발생해야 한다") {
        val userId = ObjectId()
        val petId = ObjectId()

        every {
            petRepository.findByIdAndOwnerUserIdOrThrow(
                petId,
                userId
            )
        } throws PetNotFoundException("Pet not found")

        shouldThrow<PetNotFoundException> {
            petService.getMyPet(userId, petId)
        }
    }

    test("예외 처리: 존재하지 않는 펫을 조회하려 하면 예외가 발생해야 한다") {
        val userId = ObjectId()
        val petId = ObjectId()

        every {
            petRepository.findByIdAndOwnerUserIdOrThrow(
                petId,
                userId
            )
        } throws PetNotFoundException("Pet not found")

        shouldThrow<PetNotFoundException> {
            petService.getMyPet(userId, petId)
        }
    }

    context("gachaPet") {
        test("가챠 펫: 티켓이 충분할 때 펫을 성공적으로 추가해야 한다") {
            val userId = ObjectId()
            val existingPet = mockk<Pet>()
            val savedPet = mockk<Pet>()

            every { existingPet.type } returns PetType.DOG
            every { petRepository.findAllByOwnerUserId(userId) } returns listOf(existingPet)
            every { petRepository.save(any()) } returns savedPet
            every { userRepository.decreaseTickets(userId, 1) } returns Unit

            val result = petService.gachaPet(userId)

            verify { petRepository.findAllByOwnerUserId(userId) }
            verify { petRepository.save(any()) }
            verify { userRepository.decreaseTickets(userId, 1) }
            result shouldBe savedPet
        }

        test("가챠 펫: 티켓이 부족할 때 UserTicketLackException을 발생시켜야 한다") {
            val userId = ObjectId()
            val existingPet = mockk<Pet>()
            val savedPet = mockk<Pet>()

            every { existingPet.type } returns PetType.DOG
            every { petRepository.findAllByOwnerUserId(userId) } returns listOf(existingPet)
            every { petRepository.save(any()) } returns savedPet
            every { userRepository.decreaseTickets(userId, 1) } throws UserTicketLackException("티켓이 부족합니다")

            shouldThrow<UserTicketLackException> {
                petService.gachaPet(userId)
            }

            verify { petRepository.findAllByOwnerUserId(userId) }
            verify { petRepository.save(any()) }
            verify { userRepository.decreaseTickets(userId, 1) }
        }

        test("가챠 펫: 티켓 차감 시 사용자가 존재하지 않을 때 UserNotFoundException을 발생시켜야 한다") {
            val userId = ObjectId()
            val existingPet = mockk<Pet>()
            val savedPet = mockk<Pet>()

            every { existingPet.type } returns PetType.DOG
            every { petRepository.findAllByOwnerUserId(userId) } returns listOf(existingPet)
            every { petRepository.save(any()) } returns savedPet
            every { userRepository.decreaseTickets(userId, 1) } throws UserNotFoundException("유저를 찾을 수 없습니다")

            shouldThrow<UserNotFoundException> {
                petService.gachaPet(userId)
            }

            verify { petRepository.findAllByOwnerUserId(userId) }
            verify { petRepository.save(any()) }
            verify { userRepository.decreaseTickets(userId, 1) }
        }
    }
})
