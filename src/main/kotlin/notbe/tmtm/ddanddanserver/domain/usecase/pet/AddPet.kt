package notbe.tmtm.ddanddanserver.domain.usecase.pet

import notbe.tmtm.ddanddanserver.domain.gateway.PetGateway
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.pet.PetType
import notbe.tmtm.ddanddanserver.domain.usecase.UseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AddPet(
    private val petGateway: PetGateway,
) : UseCase<AddPet.Input, AddPet.Output> {
    data class Input(
        val ownerUserId: String,
        val petType: PetType,
    )

    data class Output(
        val pet: Pet,
    )

    @Transactional
    override fun execute(input: Input): Output =
        Output(
            petGateway.save(
                Pet.register(
                    type = input.petType,
                    ownerUserId = input.ownerUserId,
                ),
            ),
        )
}
