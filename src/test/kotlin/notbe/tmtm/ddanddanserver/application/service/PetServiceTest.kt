package notbe.tmtm.ddanddanserver.application.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import notbe.tmtm.ddanddanserver.domain.exception.PetNotFoundException
import notbe.tmtm.ddanddanserver.domain.exception.UserNotFoundException
import notbe.tmtm.ddanddanserver.domain.exception.UserFoodQuantityLackException
import notbe.tmtm.ddanddanserver.domain.exception.UserTicketLackException
import notbe.tmtm.ddanddanserver.domain.exception.UserToyQuantityLackException
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.PetRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.findByIdAndOwnerUserIdOrThrow
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.findByIdOrThrow
import org.bson.types.ObjectId
import org.springframework.dao.DataAccessResourceFailureException

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

    context("feedPet") {
        val ownerUserId = ObjectId()
        val petId = ObjectId()

        beforeEach {
            every { userRepository.findByIdOrThrow(ownerUserId) } returns user(ownerUserId)
            every { petRepository.findByIdAndOwnerUserIdOrThrow(petId, ownerUserId) } returns pet(petId, ownerUserId, 0)
        }

        test("먹이를 DB에서 원자적으로 1개 차감하고 경험치를 100 올려야 한다") {
            val consumedUser = user(ownerUserId, foodQuantity = 4)
            val fedPet = pet(petId, ownerUserId, exp = 100)
            every { userRepository.consumeFood(ownerUserId, 1) } returns consumedUser
            every { petRepository.increaseExp(petId, Pet.EXP_PER_FOOD) } returns fedPet

            val result = petService.feedPet(ownerUserId, petId)

            result.user shouldBe consumedUser
            result.pet shouldBe fedPet
            verify(exactly = 0) { userRepository.increaseItems(any(), any(), any(), any()) }
            verify(exactly = 0) { userRepository.save(any()) }
            verify(exactly = 0) { petRepository.save(any()) }
        }

        test("먹이로 레벨 5에 도달하면 티켓을 1장 지급해야 한다") {
            val ticketUser = user(ownerUserId, tickets = 1)
            every { userRepository.consumeFood(ownerUserId, 1) } returns user(ownerUserId)
            every { petRepository.increaseExp(petId, Pet.EXP_PER_FOOD) } returns pet(petId, ownerUserId, exp = 4000)
            every { userRepository.increaseItems(ownerUserId, tickets = 1) } returns ticketUser

            val result = petService.feedPet(ownerUserId, petId)

            result.user shouldBe ticketUser
            result.pet.getLevel() shouldBe 5
        }

        test("먹이가 부족하면 UserFoodQuantityLackException이 발생하고 경험치는 오르지 않아야 한다") {
            every { userRepository.consumeFood(ownerUserId, 1) } throws UserFoodQuantityLackException()

            shouldThrow<UserFoodQuantityLackException> {
                petService.feedPet(ownerUserId, petId)
            }

            verify(exactly = 0) { petRepository.increaseExp(any(), any()) }
        }

        test("경험치 반영이 실패하면 차감한 먹이를 돌려주고 예외를 그대로 던져야 한다") {
            every { userRepository.consumeFood(ownerUserId, 1) } returns user(ownerUserId)
            every { petRepository.increaseExp(petId, Pet.EXP_PER_FOOD) } throws DataAccessResourceFailureException("db down")
            every { userRepository.increaseItems(ownerUserId, food = 1) } returns user(ownerUserId, foodQuantity = 1)

            shouldThrow<DataAccessResourceFailureException> {
                petService.feedPet(ownerUserId, petId)
            }

            verify(exactly = 1) { userRepository.increaseItems(ownerUserId, food = 1) }
        }

        test("먹이 환불까지 실패해도 원래 예외를 던져야 한다") {
            every { userRepository.consumeFood(ownerUserId, 1) } returns user(ownerUserId)
            every { petRepository.increaseExp(petId, Pet.EXP_PER_FOOD) } throws DataAccessResourceFailureException("db down")
            every { userRepository.increaseItems(ownerUserId, food = 1) } throws DataAccessResourceFailureException("still down")

            shouldThrow<DataAccessResourceFailureException> {
                petService.feedPet(ownerUserId, petId)
            }.message shouldBe "db down"
        }

        test("레벨업 티켓 지급이 실패해도 먹이주기는 성공으로 응답해야 한다") {
            val consumedUser = user(ownerUserId)
            every { userRepository.consumeFood(ownerUserId, 1) } returns consumedUser
            every { petRepository.increaseExp(petId, Pet.EXP_PER_FOOD) } returns pet(petId, ownerUserId, exp = 4000)
            every { userRepository.increaseItems(ownerUserId, tickets = 1) } throws DataAccessResourceFailureException("db down")

            val result = petService.feedPet(ownerUserId, petId)

            result.user shouldBe consumedUser
            result.pet.exp shouldBe 4000
        }

        test("내 펫이 아니면 먹이를 차감하지 않아야 한다") {
            every { petRepository.findByIdAndOwnerUserIdOrThrow(petId, ownerUserId) } throws PetNotFoundException()

            shouldThrow<PetNotFoundException> {
                petService.feedPet(ownerUserId, petId)
            }

            verify(exactly = 0) { userRepository.consumeFood(any(), any()) }
        }
    }

    context("playPet") {
        val ownerUserId = ObjectId()
        val petId = ObjectId()

        beforeEach {
            every { userRepository.findByIdOrThrow(ownerUserId) } returns user(ownerUserId)
            every { petRepository.findByIdAndOwnerUserIdOrThrow(petId, ownerUserId) } returns pet(petId, ownerUserId, 0)
        }

        test("장난감을 DB에서 원자적으로 1개 차감하고 경험치를 500 올려야 한다") {
            val consumedUser = user(ownerUserId, toyQuantity = 2)
            val playedPet = pet(petId, ownerUserId, exp = 500)
            every { userRepository.consumeToy(ownerUserId, 1) } returns consumedUser
            every { petRepository.increaseExp(petId, Pet.EXP_PER_TOY) } returns playedPet

            val result = petService.playPet(ownerUserId, petId)

            result.user shouldBe consumedUser
            result.pet shouldBe playedPet
            verify(exactly = 0) { userRepository.increaseItems(any(), any(), any(), any()) }
        }

        test("놀아주기로 레벨 5에 도달하면 티켓을 1장 지급해야 한다") {
            val ticketUser = user(ownerUserId, tickets = 1)
            every { userRepository.consumeToy(ownerUserId, 1) } returns user(ownerUserId)
            every { petRepository.increaseExp(petId, Pet.EXP_PER_TOY) } returns pet(petId, ownerUserId, exp = 4100)
            every { userRepository.increaseItems(ownerUserId, tickets = 1) } returns ticketUser

            val result = petService.playPet(ownerUserId, petId)

            result.user shouldBe ticketUser
        }

        test("레벨 6 이상에서는 놀아줘도 티켓을 다시 지급하지 않아야 한다") {
            every { userRepository.consumeToy(ownerUserId, 1) } returns user(ownerUserId)
            every { petRepository.increaseExp(petId, Pet.EXP_PER_TOY) } returns pet(petId, ownerUserId, exp = 8600)

            val result = petService.playPet(ownerUserId, petId)

            result.pet.getLevel() shouldBe 6
            verify(exactly = 0) { userRepository.increaseItems(any(), any(), any(), any()) }
        }

        test("장난감이 부족하면 UserToyQuantityLackException이 발생해야 한다") {
            every { userRepository.consumeToy(ownerUserId, 1) } throws UserToyQuantityLackException()

            shouldThrow<UserToyQuantityLackException> {
                petService.playPet(ownerUserId, petId)
            }

            verify(exactly = 0) { petRepository.increaseExp(any(), any()) }
        }

        test("경험치 반영이 실패하면 차감한 장난감을 돌려줘야 한다") {
            every { userRepository.consumeToy(ownerUserId, 1) } returns user(ownerUserId)
            every { petRepository.increaseExp(petId, Pet.EXP_PER_TOY) } throws DataAccessResourceFailureException("db down")
            every { userRepository.increaseItems(ownerUserId, toy = 1) } returns user(ownerUserId, toyQuantity = 1)

            shouldThrow<DataAccessResourceFailureException> {
                petService.playPet(ownerUserId, petId)
            }

            verify(exactly = 1) { userRepository.increaseItems(ownerUserId, toy = 1) }
        }
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

            verifyOrder {
                userRepository.decreaseTickets(userId, 1)
                petRepository.save(any())
            }
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

            verify { userRepository.decreaseTickets(userId, 1) }
            verify(exactly = 0) { petRepository.save(any()) }
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

            verify(exactly = 0) { petRepository.save(any()) }
        }

        test("가챠 펫: 펫 생성이 실패하면 차감한 티켓을 돌려줘야 한다") {
            val userId = ObjectId()

            every { petRepository.findAllByOwnerUserId(userId) } returns emptyList()
            every { petCatalogService.pickRandomActiveExcluding(emptyList()) } returns "CAT"
            every { userRepository.decreaseTickets(userId, 1) } returns Unit
            every { petRepository.save(any()) } throws DataAccessResourceFailureException("db down")
            every { userRepository.increaseTickets(userId, 1) } returns Unit

            shouldThrow<DataAccessResourceFailureException> {
                petService.gachaPet(userId)
            }

            verify(exactly = 1) { userRepository.increaseTickets(userId, 1) }
        }
    }
})

private fun user(
    id: ObjectId,
    foodQuantity: Int = 0,
    toyQuantity: Int = 0,
    tickets: Int = 0,
) = User(
    id = id,
    deviceToken = null,
    name = "testUser",
    foodQuantity = foodQuantity,
    toyQuantity = toyQuantity,
    tickets = tickets,
)

private fun pet(id: ObjectId, ownerUserId: ObjectId, exp: Int) =
    Pet(id = id, type = "DOG", ownerUserId = ownerUserId, exp = exp)
