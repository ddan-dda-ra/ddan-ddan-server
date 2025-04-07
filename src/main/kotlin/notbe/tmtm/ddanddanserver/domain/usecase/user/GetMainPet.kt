package notbe.tmtm.ddanddanserver.domain.usecase.user

import notbe.tmtm.ddanddanserver.domain.gateway.PetGateway
import notbe.tmtm.ddanddanserver.domain.gateway.UserGateway
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.usecase.UseCase
import org.bson.types.ObjectId
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetMainPet(
    private val userGateway: UserGateway,
    private val petGateway: PetGateway,
) : UseCase<GetMainPet.Input, GetMainPet.Output> {
    data class Input(
        val userId: ObjectId,
    )

    data class Output(
        val mainPet: Pet?,
    )

    @Transactional(readOnly = true)
    override fun execute(input: Input): Output {
        val user = userGateway.getById(input.userId)
        return Output(mainPet = user.mainPetId?.let { petGateway.getById(it) })
    }
}
