package notbe.tmtm.ddanddanserver.infrastructure.gateway

import notbe.tmtm.ddanddanserver.domain.exception.PetNotFoundException
import notbe.tmtm.ddanddanserver.domain.gateway.PetGateway
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.PetRepository
import org.bson.types.ObjectId
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class PetGatewayImpl(
    private val petRepository: PetRepository,
) : PetGateway {
    override fun save(pet: Pet): Pet = petRepository.save(pet)

    override fun getAll(): List<Pet> = petRepository.findAll()

    override fun getById(petId: ObjectId): Pet =
        petRepository.findByIdOrNull(petId)
            ?: throw PetNotFoundException("Pet not found with id: $petId")

    override fun getPetsByOwnerUserId(ownerUserId: ObjectId): List<Pet> =
        petRepository.findAllByOwnerUserId(ownerUserId)
}
