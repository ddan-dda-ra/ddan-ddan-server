package notbe.tmtm.ddanddanserver.infrastructure.gateway

import notbe.tmtm.ddanddanserver.domain.gateway.PetGateway
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.PetRepository
import org.bson.types.ObjectId
import org.springframework.stereotype.Component

@Component
class PetGatewayImpl(
    private val petRepository: PetRepository,
) : PetGateway {
    override fun save(pet: Pet) = petRepository.save(pet)

    override fun getAll(): List<Pet> = petRepository.findAll()

    override fun getById(petId: ObjectId): Pet = petRepository.findById(petId).orElseThrow()

    override fun getPetsByOwnerUserId(ownerUserId: ObjectId) = petRepository.findAllByOwnerUserId(ownerUserId)
}
