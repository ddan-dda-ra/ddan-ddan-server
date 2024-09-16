package notbe.tmtm.ddanddanserver.domain.usecase.pet

import notbe.tmtm.ddanddanserver.domain.gateway.PetGateway
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.usecase.UseCase
import notbe.tmtm.ddanddanserver.domain.usecase.pet.GetPets.Input
import notbe.tmtm.ddanddanserver.domain.usecase.pet.GetPets.Output
import org.springframework.stereotype.Component

@Component
class GetPets(
    private val petGateway: PetGateway,
) : UseCase<Input, Output> {
    data class Input(
        val ownerUserId: String,
    )

    data class Output(
        val pets: List<Pet>,
    )

    override fun execute(input: Input): Output =
        Output(
            petGateway.getPetsByOwnerUserId(input.ownerUserId),
        )
}
