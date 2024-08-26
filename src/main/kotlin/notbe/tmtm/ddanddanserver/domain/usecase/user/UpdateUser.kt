package notbe.tmtm.ddanddanserver.domain.usecase.user

import notbe.tmtm.ddanddanserver.domain.gateway.UserGateway
import notbe.tmtm.ddanddanserver.domain.model.User
import notbe.tmtm.ddanddanserver.domain.usecase.UseCase
import notbe.tmtm.ddanddanserver.domain.usecase.user.UpdateUser.UpdateUserInput
import notbe.tmtm.ddanddanserver.domain.usecase.user.UpdateUser.UpdateUserOutput
import org.springframework.stereotype.Component

@Component
class UpdateUser(
    private val userGateway: UserGateway,
) : UseCase<UpdateUserInput, UpdateUserOutput> {
    data class UpdateUserInput(
        val userId: String,
        val name: String,
        val purposeCalorie: Int,
    )

    data class UpdateUserOutput(
        val user: User,
    )

    override fun execute(input: UpdateUserInput): UpdateUserOutput {
        val user = userGateway.getById(input.userId)

        user.update(
            name = input.name,
            purposeCalorie = input.purposeCalorie,
        )

        return UpdateUserOutput(
            userGateway.update(user),
        )
    }
}
