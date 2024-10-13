package notbe.tmtm.ddanddanserver.domain.usecase.user

import notbe.tmtm.ddanddanserver.domain.exception.PetOwnerMismatchException
import notbe.tmtm.ddanddanserver.domain.gateway.PetGateway
import notbe.tmtm.ddanddanserver.domain.gateway.UserGateway
import notbe.tmtm.ddanddanserver.domain.model.User
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.usecase.UseCase
import org.springframework.stereotype.Component

@Component
class SetMainPet(
    private val userGateway: UserGateway,
    private val petGateway: PetGateway,
) : UseCase<SetMainPet.Input, SetMainPet.Output> {
    data class Input(
        val ownerUserId: String,
        val petId: String,
    )

    data class Output(
        val user: User,
        val pet: Pet,
    )

    override fun execute(input: Input): Output {
        val user = userGateway.getById(input.ownerUserId)
        val pet = petGateway.getById(input.petId)
        validate(pet, user)

        user.setMainPet(pet.id)
        userGateway.save(user)
        return Output(user, pet)
    }

    private fun validate(
        pet: Pet,
        user: User,
    ) {
        when {
            pet.isOwner(user.id).not() -> {
                throw PetOwnerMismatchException()
            }
        }
    }
}
