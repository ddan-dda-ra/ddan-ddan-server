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
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.PetRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.findByIdAndOwnerUserIdOrThrow
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.findByIdOrThrow
import org.bson.types.ObjectId

class PetServiceTest : FunSpec({
    lateinit var petRepository: PetRepository
    lateinit var userRepository: UserRepository
    lateinit var petCatalogService: PetCatalogService
    lateinit var petService: PetService

    beforeEach {
        petRepository = mockk()
        userRepository = mockk()
        petCatalogService = mockk(relaxed = true)
        petService = PetService(petRepository, userRepository, petCatalogService)
    }

    test("펫 추가: 펫을 성공적으로 추가해야 한다") {
        val ownerUserId = ObjectId()
        val petType = "DOG"
        val savedPet = mockk<Pet>()

        every { petCatalogService.requireActive(petType) } returns Unit
        every { petRepository.save(any()) } returns savedPet

        val result = petService.addPet(ownerUserId, petType)

        verify { petCatalogService.requireActive(petType) }
        verify { petRepository.save(any()) }
        result shouldBe savedPet
    }

    test("랜덤 펫 추가: 랜덤 펫을 성공적으로 추가해야 한다") {
        val ownerUserId = ObjectId()
        val existingPet = mockk<Pet>()
        val savedPet = mockk<Pet>()

        every { existingPet.type } returns "DOG"
        every { petRepository.findAllByOwnerUserId(ownerUserId) } returns listOf(existingPet)
        every { petCatalogService.pickRandomActiveExcluding(listOf("DOG")) } returns "CAT"
        every { petRepository.save(any()) } returns savedPet

        val result = petService.addRandomPet(ownerUserId)

        verify { petRepository.findAllByOwnerUserId(ownerUserId) }
        verify { petCatalogService.pickRandomActiveExcluding(listOf("DOG")) }
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

        verify { user.feed(pet) }
        verify { userRepository.save(user) }
        verify { petRepository.save(pet) }
        result.user shouldBe savedUser
        result.pet shouldBe savedPet
    }

    test("펫 먹이주기: 레벨업 시 티켓을 받아야 한다") {
        val ownerUserId = ObjectId()
        val petId = ObjectId()
        val user = User.register("testToken", "testUser")
        val pet = Pet.register("DOG", ownerUserId)
        pet.exp = 3900 // 레벨 4, 레벨 5까지 100 경험치 필요
        user.foodQuantity = 5

        every { userRepository.findByIdOrThrow(ownerUserId) } returns user
        every { petRepository.findByIdAndOwnerUserIdOrThrow(petId, ownerUserId) } returns pet
        every { userRepository.save(user) } returns user
        every { petRepository.save(pet) } returns pet

        val result = petService.feedPet(ownerUserId, petId)

        pet.exp shouldBe 4000 // 3900 + 100
        pet.getLevel() shouldBe 5
        user.tickets shouldBe 1
        user.foodQuantity shouldBe 4
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

        verify { user.play(pet) }
        verify { userRepository.save(user) }
        verify { petRepository.save(pet) }
        result.user shouldBe savedUser
        result.pet shouldBe savedPet
    }

    test("펫 놀아주기: 레벨업 시 티켓을 받아야 한다") {
        val ownerUserId = ObjectId()
        val petId = ObjectId()
        val user = User.register("testToken", "testUser")
        val pet = Pet.register("DOG", ownerUserId)
        pet.exp = 3600 // 레벨 4, 레벨 5까지 400 경험치 필요
        user.toyQuantity = 3

        every { userRepository.findByIdOrThrow(ownerUserId) } returns user
        every { petRepository.findByIdAndOwnerUserIdOrThrow(petId, ownerUserId) } returns pet
        every { userRepository.save(user) } returns user
        every { petRepository.save(pet) } returns pet

        val result = petService.playPet(ownerUserId, petId)

        pet.exp shouldBe 4100 // 3600 + 500
        pet.getLevel() shouldBe 5
        user.tickets shouldBe 1
        user.toyQuantity shouldBe 2
    }

    test("레벨 6 이상 펫도 먹이주기와 놀아주기를 계속할 수 있다") {
        val ownerUserId = ObjectId()
        val petId = ObjectId()
        val user = User.register("testToken", "testUser").apply {
            foodQuantity = 1
            toyQuantity = 1
        }
        val pet = Pet.register("DOG", ownerUserId).apply { exp = 8000 }
        every { userRepository.findByIdOrThrow(ownerUserId) } returns user
        every { petRepository.findByIdAndOwnerUserIdOrThrow(petId, ownerUserId) } returns pet
        every { userRepository.save(user) } returns user
        every { petRepository.save(pet) } returns pet

        petService.feedPet(ownerUserId, petId)
        petService.playPet(ownerUserId, petId)

        pet.exp shouldBe 8600
        pet.getLevel() shouldBe 6
        pet.isMaxLevel() shouldBe false
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

            every { existingPet.type } returns "DOG"
            every { petRepository.findAllByOwnerUserId(userId) } returns listOf(existingPet)
            every { petCatalogService.pickRandomActiveExcluding(listOf("DOG")) } returns "CAT"
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

            every { existingPet.type } returns "DOG"
            every { petRepository.findAllByOwnerUserId(userId) } returns listOf(existingPet)
            every { petCatalogService.pickRandomActiveExcluding(listOf("DOG")) } returns "CAT"
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

            every { existingPet.type } returns "DOG"
            every { petRepository.findAllByOwnerUserId(userId) } returns listOf(existingPet)
            every { petCatalogService.pickRandomActiveExcluding(listOf("DOG")) } returns "CAT"
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
