package notbe.tmtm.ddanddanserver.domain.usecase.pet

import notbe.tmtm.ddanddanserver.domain.exception.PetOwnerMismatchException
import notbe.tmtm.ddanddanserver.domain.gateway.PetGateway
import notbe.tmtm.ddanddanserver.domain.model.pet.Pet
import notbe.tmtm.ddanddanserver.domain.usecase.UseCase
import org.bson.types.ObjectId
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetPet(
    private val petGateway: PetGateway,
) : UseCase<GetPet.Input, GetPet.Output> {
    data class Input(
        val userId: ObjectId,
        val petId: ObjectId,
    )

    data class Output(
        val pet: Pet,
    )

    @Transactional(readOnly = true)
    override fun execute(input: Input): Output {
        val pet = petGateway.getById(input.petId)
        validate(input.userId, pet.ownerUserId)

        return Output(pet)
    }

    private fun validate(
        userId: ObjectId,
        ownerUserId: ObjectId,
    ) {
        if (userId != ownerUserId) {
            throw PetOwnerMismatchException("펫의 주인이 아닙니다.")
        }
    }
}
