package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface PetRepository : MongoRepository<Pet, ObjectId> {
    fun findAllByOwnerUserId(ownerUserId: ObjectId): List<Pet>

    fun deleteAllByOwnerUserId(ownerUserId: ObjectId)
}
