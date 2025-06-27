package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.domain.exception.PetMaxLevelException
import notbe.tmtm.ddanddanserver.domain.exception.PetOwnerMismatchException
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.pet.PetType
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.PetRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.findByIdOrThrow
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PetService(
    private val petRepository: PetRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun addPet(ownerUserId: ObjectId, petType: PetType): Pet {
        return addPet(petType, ownerUserId)
    }

    @Transactional
    fun addRandomPet(ownerUserId: ObjectId): Pet {
        val existingPetTypes = petRepository.findAllByOwnerUserId(ownerUserId)
            .map { it.type }
            .distinct()
        
        return addPet(PetType.getRandomWithout(existingPetTypes), ownerUserId)
    }

    @Transactional
    fun feedPet(ownerUserId: ObjectId, petId: ObjectId): FeedPetResult {
        val user = userRepository.findByIdOrThrow(ownerUserId)
        val pet = petRepository.findByIdOrThrow(petId)

        validatePetOwnership(pet, ownerUserId)
        validatePetNotMaxLevel(pet)

        pet.eat()
        user.feed()

        return FeedPetResult(
            user = userRepository.save(user),
            pet = petRepository.save(pet),
        )
    }

    @Transactional
    fun playPet(ownerUserId: ObjectId, petId: ObjectId): PlayPetResult {
        val user = userRepository.findByIdOrThrow(ownerUserId)
        val pet = petRepository.findByIdOrThrow(petId)

        validatePetOwnership(pet, ownerUserId)
        validatePetNotMaxLevel(pet)

        pet.play()
        user.play()

        return PlayPetResult(
            user = userRepository.save(user),
            pet = petRepository.save(pet),
        )
    }

    @Transactional(readOnly = true)
    fun getPet(userId: ObjectId, petId: ObjectId): Pet {
        val pet = petRepository.findByIdOrThrow(petId)
        validatePetOwnership(pet, userId)
        return pet
    }

    @Transactional(readOnly = true)
    fun getPetsByOwner(ownerUserId: ObjectId): List<Pet> {
        return petRepository.findAllByOwnerUserId(ownerUserId)
    }

    private fun addPet(
        petType: PetType,
        ownerUserId: ObjectId
    ) = petRepository.save(
        Pet.register(
            type = petType,
            ownerUserId = ownerUserId,
        ),
    )

    private fun validatePetOwnership(pet: Pet, userId: ObjectId) {
        if (!pet.isOwner(userId)) {
            throw PetOwnerMismatchException("펫의 주인이 아닙니다.")
        }
    }

    private fun validatePetNotMaxLevel(pet: Pet) {
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
