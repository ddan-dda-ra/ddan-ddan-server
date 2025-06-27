package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.domain.exception.PetNotFoundException
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.findByIdOrNull

interface PetRepository : MongoRepository<Pet, ObjectId> {
    fun findAllByOwnerUserId(ownerUserId: ObjectId): List<Pet>

    fun deleteAllByOwnerUserId(ownerUserId: ObjectId)
}

fun PetRepository.findByIdOrThrow(petId: ObjectId): Pet {
    return findByIdOrNull(petId)
        ?: throw PetNotFoundException("Pet not found with id: $petId")
}
