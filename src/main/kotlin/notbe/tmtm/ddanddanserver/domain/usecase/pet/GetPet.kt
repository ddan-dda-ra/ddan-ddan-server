package notbe.tmtm.ddanddanserver.domain.usecase.pet

import notbe.tmtm.ddanddanserver.domain.exception.PetOwnerMismatchException
import notbe.tmtm.ddanddanserver.domain.gateway.PetGateway
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.usecase.UseCase
import org.springframework.stereotype.Component

@Component
class GetPet(
    private val petGateway: PetGateway,
) : UseCase<GetPet.Input, GetPet.Output> {
    data class Input(
        val userId: String,
        val petId: String,
    )

    data class Output(
        val pet: Pet,
    )

    override fun execute(input: Input): Output {
        val pet = petGateway.getById(input.petId)
        validate(input.userId, pet.ownerUserId)

        return Output(pet)
    }

    private fun validate(
        userId: String,
        ownerUserId: String,
    ) {
        if (userId != ownerUserId) {
            throw PetOwnerMismatchException("펫의 주인이 아닙니다.")
        }
    }
}
