package notbe.tmtm.ddanddanserver.domain.usecase.pet

import notbe.tmtm.ddanddanserver.domain.exception.PetMaxLevelException
import notbe.tmtm.ddanddanserver.domain.exception.PetOwnerMismatchException
import notbe.tmtm.ddanddanserver.domain.gateway.PetGateway
import notbe.tmtm.ddanddanserver.domain.gateway.UserGateway
import notbe.tmtm.ddanddanserver.domain.model.User
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.usecase.UseCase
import org.springframework.stereotype.Component

@Component
class PlayPet(
    private val userGateway: UserGateway,
    private val petGateway: PetGateway,
) : UseCase<PlayPet.Input, PlayPet.Output> {
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
        validate(pet, input)

        play(user, pet)

        return Output(userGateway.save(user), petGateway.save(pet))
    }

    private fun validate(
        pet: Pet,
        input: Input,
    ) {
        when {
            pet.isOwner(input.ownerUserId).not() -> {
                throw PetOwnerMismatchException("펫의 주인이 아닙니다.")
            }

            pet.isMaxLevel() -> {
                throw PetMaxLevelException("펫이 최대 레벨입니다.")
            }
        }
    }

    private fun play(
        user: User,
        pet: Pet,
    ) {
        pet.play()
        user.play()
    }
}
