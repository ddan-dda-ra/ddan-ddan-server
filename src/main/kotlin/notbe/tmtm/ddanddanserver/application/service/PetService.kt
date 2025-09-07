package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.pet.PetType
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
        val pet = petRepository.findByIdAndOwnerUserIdOrThrow(petId, ownerUserId)

        user.feed(pet)

        return FeedPetResult(
            user = userRepository.save(user),
            pet = petRepository.save(pet),
        )
    }

    @Transactional
    fun playPet(ownerUserId: ObjectId, petId: ObjectId): PlayPetResult {
        val user = userRepository.findByIdOrThrow(ownerUserId)
        val pet = petRepository.findByIdAndOwnerUserIdOrThrow(petId, ownerUserId)

        user.play(pet)

        return PlayPetResult(
            user = userRepository.save(user),
            pet = petRepository.save(pet),
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

        val addedPet = addPet(PetType.getRandomWithout(existingPetTypes), userId)
        userRepository.decreaseTickets(userId, 1)

        return addedPet
    }

    private fun addPet(
        petType: PetType,
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
