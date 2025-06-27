package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.domain.exception.PetMaxLevelException
import notbe.tmtm.ddanddanserver.domain.exception.PetOwnerMismatchException
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.pet.PetType
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.PetRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import org.bson.types.ObjectId
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PetService(
    private val petRepository: PetRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun addPet(
        ownerUserId: ObjectId,
        petType: PetType,
    ): Pet {
        return petRepository.save(
            Pet.register(
                type = petType,
                ownerUserId = ownerUserId,
            ),
        )
    }

    @Transactional
    fun addRandomPet(ownerUserId: ObjectId): Pet {
        val randomPetType = PetType.values().random()
        return petRepository.save(
            Pet.register(
                type = randomPetType,
                ownerUserId = ownerUserId,
            ),
        )
    }

    @Transactional
    fun feedPet(
        ownerUserId: ObjectId,
        petId: ObjectId,
    ): FeedPetResult {
        val user = userRepository.findByIdOrNull(ownerUserId) ?: throw IllegalArgumentException("User not found")
        val pet = petRepository.findByIdOrNull(petId) ?: throw IllegalArgumentException("Pet not found")
        validatePetOwnership(pet, ownerUserId)
        validatePetMaxLevel(pet)

        pet.eat()
        user.feed()

        return FeedPetResult(userRepository.save(user), petRepository.save(pet))
    }

    @Transactional
    fun playPet(
        ownerUserId: ObjectId,
        petId: ObjectId,
    ): PlayPetResult {
        val user = userRepository.findByIdOrNull(ownerUserId) ?: throw IllegalArgumentException("User not found")
        val pet = petRepository.findByIdOrNull(petId) ?: throw IllegalArgumentException("Pet not found")
        validatePetOwnership(pet, ownerUserId)
        validatePetMaxLevel(pet)

        pet.play()
        user.play()

        return PlayPetResult(userRepository.save(user), petRepository.save(pet))
    }

    fun getPets(ownerUserId: ObjectId): List<Pet> {
        return petRepository.findAllByOwnerUserId(ownerUserId)
    }

    fun getPet(
        userId: ObjectId,
        petId: ObjectId,
    ): Pet {
        val pet = petRepository.findByIdOrNull(petId) ?: throw IllegalArgumentException("Pet not found")
        validatePetOwnership(pet, userId)
        return pet
    }

    private fun validatePetOwnership(
        pet: Pet,
        ownerUserId: ObjectId,
    ) {
        if (!pet.isOwner(ownerUserId)) {
            throw PetOwnerMismatchException("펫의 주인이 아닙니다.")
        }
    }

    private fun validatePetMaxLevel(pet: Pet) {
        if (pet.isMaxLevel()) {
            throw PetMaxLevelException("펫이 최대 레벨입니다.")
        }
    }

    data class FeedPetResult(
        val user: User,
        val pet: Pet,
    )

    data class PlayPetResult(
        val user: User,
        val pet: Pet,
    )
}
