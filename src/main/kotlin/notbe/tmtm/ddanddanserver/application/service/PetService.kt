package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.domain.exception.PetMaxLevelException
import notbe.tmtm.ddanddanserver.domain.exception.PetNotFoundException
import notbe.tmtm.ddanddanserver.domain.exception.PetOwnerMismatchException
import notbe.tmtm.ddanddanserver.domain.exception.UserNotFoundException
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
    fun addPet(ownerUserId: ObjectId, petType: PetType): Pet {
        return petRepository.save(
            Pet.register(
                type = petType,
                ownerUserId = ownerUserId,
            ),
        )
    }

    @Transactional
    fun addRandomPet(ownerUserId: ObjectId): Pet {
        val existingPetTypes = petRepository.findAllByOwnerUserId(ownerUserId)
            .map { it.type }
            .distinct()
        
        return petRepository.save(
            Pet.register(
                type = PetType.getRandomWithout(existingPetTypes),
                ownerUserId = ownerUserId,
            ),
        )
    }

    @Transactional
    fun feedPet(ownerUserId: ObjectId, petId: ObjectId): FeedPetResult {
        val user = getUserByIdOrThrow(ownerUserId)
        val pet = getPetByIdOrThrow(petId)
        
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
        val user = getUserByIdOrThrow(ownerUserId)
        val pet = getPetByIdOrThrow(petId)
        
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
        val pet = getPetByIdOrThrow(petId)
        validatePetOwnership(pet, userId)
        return pet
    }

    @Transactional(readOnly = true)
    fun getPetsByOwner(ownerUserId: ObjectId): List<Pet> {
        return petRepository.findAllByOwnerUserId(ownerUserId)
    }

    private fun getUserByIdOrThrow(userId: ObjectId): User {
        return userRepository.findByIdOrNull(userId)
            ?: throw UserNotFoundException("User not found with id: $userId")
    }

    private fun getPetByIdOrThrow(petId: ObjectId): Pet {
        return petRepository.findByIdOrNull(petId)
            ?: throw PetNotFoundException("Pet not found with id: $petId")
    }

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
