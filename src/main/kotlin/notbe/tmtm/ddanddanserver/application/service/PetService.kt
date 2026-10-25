package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.common.util.logger
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.PetRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.findByIdAndOwnerUserIdOrThrow
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.findByIdOrThrow
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PetService(
    private val petRepository: PetRepository,
    private val userRepository: UserRepository,
    private val petCatalogService: PetCatalogService,
) {
    private val log = logger()

    @Transactional
    fun addPet(ownerUserId: ObjectId, petType: String): Pet {
        val canonicalType = petCatalogService.requireActive(petType)
        return addPetInternal(canonicalType, ownerUserId)
    }

    @Transactional
    fun addRandomPet(ownerUserId: ObjectId): Pet {
        val existingPetTypes = petRepository.findAllByOwnerUserId(ownerUserId)
            .map { it.type }
            .distinct()

        val picked = petCatalogService.pickRandomActiveExcluding(existingPetTypes)
        return addPetInternal(picked, ownerUserId)
    }

    @Transactional
    fun feedPet(ownerUserId: ObjectId, petId: ObjectId): FeedPetResult {
        userRepository.findByIdOrThrow(ownerUserId)
        petRepository.findByIdAndOwnerUserIdOrThrow(petId, ownerUserId)

        val user = userRepository.consumeFood(ownerUserId, 1)
        val pet = increaseExpOrRefund(petId, Pet.EXP_PER_FOOD) {
            userRepository.increaseItems(ownerUserId, food = 1)
        }

        return FeedPetResult(
            user = rewardTicketIfLevelUp(user, pet, Pet.EXP_PER_FOOD),
            pet = pet,
        )
    }

    @Transactional
    fun playPet(ownerUserId: ObjectId, petId: ObjectId): PlayPetResult {
        userRepository.findByIdOrThrow(ownerUserId)
        petRepository.findByIdAndOwnerUserIdOrThrow(petId, ownerUserId)

        val user = userRepository.consumeToy(ownerUserId, 1)
        val pet = increaseExpOrRefund(petId, Pet.EXP_PER_TOY) {
            userRepository.increaseItems(ownerUserId, toy = 1)
        }

        return PlayPetResult(
            user = rewardTicketIfLevelUp(user, pet, Pet.EXP_PER_TOY),
            pet = pet,
        )
    }

    @Transactional(readOnly = true)
    fun getMyPet(userId: ObjectId, petId: ObjectId): Pet = petRepository.findByIdAndOwnerUserIdOrThrow(petId, userId)

    @Transactional(readOnly = true)
    fun getPetsByOwner(ownerUserId: ObjectId): List<Pet> {
        return petRepository.findAllByOwnerUserId(ownerUserId)
    }

    @Transactional
    fun gachaPet(userId: ObjectId): Pet {
        val existingPetTypes = petRepository.findAllByOwnerUserId(userId)
            .map { it.type }
            .distinct()

        val picked = petCatalogService.pickRandomActiveExcluding(existingPetTypes)
        userRepository.decreaseTickets(userId, 1)

        return try {
            addPetInternal(picked, userId)
        } catch (e: Exception) {
            runCatching { userRepository.increaseTickets(userId, 1) }.onFailure {
                log.error("티켓 환불 실패. userId: $userId", it)
            }
            throw e
        }
    }

    /**
     * 아이템은 이미 차감된 상태라, 경험치 반영이 실패하면 차감한 아이템을 돌려준다.
     * $inc는 멱등이 아니라서 자동 재시도하면 두 번 반영될 수 있다. 그래서 재시도 대신 되돌린 뒤 실패를 응답한다.
     */
    private fun increaseExpOrRefund(petId: ObjectId, exp: Int, refund: () -> Unit): Pet =
        try {
            petRepository.increaseExp(petId, exp)
        } catch (e: Exception) {
            runCatching(refund).onFailure {
                log.error("아이템 환불 실패. petId: $petId, exp: $exp", it)
            }
            throw e
        }

    /**
     * 먹이·경험치 반영은 끝났으므로, 티켓 지급이 실패해도 요청은 성공으로 응답한다.
     * 실패로 응답하면 클라이언트가 다시 먹이를 줘서 아이템을 한 번 더 쓰게 된다.
     */
    private fun rewardTicketIfLevelUp(user: User, pet: Pet, gainedExp: Int): User {
        if (!pet.levelUpResultSince(pet.exp - gainedExp).shouldReceiveTicket()) {
            return user
        }

        return runCatching { userRepository.increaseItems(user.id, tickets = 1) }
            .getOrElse {
                log.error("레벨업 티켓 지급 실패. userId: ${user.id}, petId: ${pet.id}", it)
                user
            }
    }

    private fun addPetInternal(
        petType: String,
        ownerUserId: ObjectId
    ) = petRepository.save(Pet.register(type = petType, ownerUserId = ownerUserId))


    data class FeedPetResult(
        val user: User,
        val pet: Pet,
    )

    data class PlayPetResult(
        val user: User,
        val pet: Pet,
    )
}
