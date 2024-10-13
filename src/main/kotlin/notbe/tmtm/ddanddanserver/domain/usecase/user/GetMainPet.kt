package notbe.tmtm.ddanddanserver.domain.usecase.user

import notbe.tmtm.ddanddanserver.domain.gateway.PetGateway
import notbe.tmtm.ddanddanserver.domain.gateway.UserGateway
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.usecase.UseCase
import org.springframework.stereotype.Component

@Component
class GetMainPet(
    private val userGateway: UserGateway,
    private val petGateway: PetGateway,
) : UseCase<GetMainPet.Input, GetMainPet.Output> {
    data class Input(
        val userId: String,
    )

    data class Output(
        val mainPet: Pet?,
    )

    override fun execute(input: Input): Output {
        val user = userGateway.getById(input.userId)
        return Output(mainPet = user.mainPetId?.let { petGateway.getById(it) })
    }
}
