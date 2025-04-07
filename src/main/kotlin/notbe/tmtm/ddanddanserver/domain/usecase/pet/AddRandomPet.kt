package notbe.tmtm.ddanddanserver.domain.usecase.pet

import notbe.tmtm.ddanddanserver.domain.gateway.PetGateway
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.model.pet.PetType
import notbe.tmtm.ddanddanserver.domain.usecase.UseCase
import org.bson.types.ObjectId
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AddRandomPet(
    private val petGateway: PetGateway,
) : UseCase<AddRandomPet.Input, AddRandomPet.Output> {
    data class Input(
        val ownerUserId: ObjectId,
    )

    data class Output(
        val pet: Pet,
    )

    @Transactional
    override fun execute(input: Input): Output {
        val pets = petGateway.getAll().map { it.type }.distinct()
        return Output(
            petGateway.save(
                Pet.register(
                    type = PetType.getRandomWithout(pets),
                    ownerUserId = input.ownerUserId,
                ),
            ),
        )
    }
}
