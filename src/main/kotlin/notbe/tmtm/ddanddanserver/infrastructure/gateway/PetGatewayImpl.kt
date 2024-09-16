package notbe.tmtm.ddanddanserver.infrastructure.gateway

import notbe.tmtm.ddanddanserver.domain.gateway.PetGateway
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.PetEntity
import notbe.tmtm.ddanddanserver.infrastructure.database.respository.PetRepository
import org.springframework.stereotype.Component

@Component
class PetGatewayImpl(
    private val petRepository: PetRepository,
) : PetGateway {
    override fun save(pet: Pet) = petRepository.save(PetEntity.fromDomain(pet)).toDomain()

    override fun getById(petId: String) = petRepository.findById(petId).orElseThrow().toDomain()

    override fun getPetsByOwnerUserId(ownerUserId: String) = petRepository.findAllByOwnerUserId(ownerUserId).map { it.toDomain() }
}
