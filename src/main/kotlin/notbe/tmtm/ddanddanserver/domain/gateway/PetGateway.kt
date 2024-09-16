package notbe.tmtm.ddanddanserver.domain.gateway

import notbe.tmtm.ddanddanserver.domain.model.pet.Pet

interface PetGateway {
    fun save(pet: Pet): Pet

    fun getById(petId: String): Pet

    fun getPetsByOwnerUserId(ownerUserId: String): List<Pet>
}
