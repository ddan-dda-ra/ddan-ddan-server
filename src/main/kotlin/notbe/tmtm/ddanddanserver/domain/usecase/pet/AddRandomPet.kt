package notbe.tmtm.ddanddanserver.domain.usecase.pet

import notbe.tmtm.ddanddanserver.domain.gateway.PetGateway
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.pet.PetType
import notbe.tmtm.ddanddanserver.domain.usecase.UseCase
import org.springframework.stereotype.Component

@Component
class AddRandomPet(
    private val petGateway: PetGateway,
) : UseCase<AddRandomPet.Input, AddRandomPet.Output> {
    data class Input(
        val ownerUserId: String,
    )

    data class Output(
        val pet: Pet,
    )

    override fun execute(input: Input): Output =
        Output(
            petGateway.save(
                Pet.register(
                    type = PetType.getRandom(),
                    ownerUserId = input.ownerUserId,
                ),
            ),
        )
}
