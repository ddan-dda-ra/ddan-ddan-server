package notbe.tmtm.ddanddanserver.domain.usecase.user

import notbe.tmtm.ddanddanserver.domain.gateway.UserGateway
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.domain.usecase.UseCase
import notbe.tmtm.ddanddanserver.domain.usecase.user.GetUser.GetUserInput
import notbe.tmtm.ddanddanserver.domain.usecase.user.GetUser.GetUserOutput
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetUser(
    private val userGateway: UserGateway,
) : UseCase<GetUserInput, GetUserOutput> {
    data class GetUserInput(
        val userId: String,
    )

    data class GetUserOutput(
        val user: User,
    )

    @Transactional(readOnly = true)
    override fun execute(input: GetUserInput): GetUserOutput =
        GetUserOutput(
            userGateway.getById(input.userId),
        )
}
