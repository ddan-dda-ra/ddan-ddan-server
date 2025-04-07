package notbe.tmtm.ddanddanserver.domain.gateway

import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import org.bson.types.ObjectId

interface PetGateway {
    fun save(pet: Pet): Pet

    fun getAll(): List<Pet>

    fun getById(petId: ObjectId): Pet

    fun getPetsByOwnerUserId(ownerUserId: ObjectId): List<Pet>

    fun deleteByUserId(userId: ObjectId)
}
