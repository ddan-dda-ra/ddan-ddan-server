package notbe.tmtm.ddanddanserver.domain.usecase.pet

import notbe.tmtm.ddanddanserver.domain.gateway.PetGateway
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.usecase.UseCase
import notbe.tmtm.ddanddanserver.domain.usecase.pet.GetPets.Input
import notbe.tmtm.ddanddanserver.domain.usecase.pet.GetPets.Output
import org.bson.types.ObjectId
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetPets(
    private val petGateway: PetGateway,
) : UseCase<Input, Output> {
    data class Input(
        val ownerUserId: ObjectId,
    )

    data class Output(
        val pets: List<Pet>,
    )

    @Transactional(readOnly = true)
    override fun execute(input: Input): Output =
        Output(
            petGateway.getPetsByOwnerUserId(input.ownerUserId),
        )
}
