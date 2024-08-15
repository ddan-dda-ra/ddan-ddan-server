package notbe.tmtm.ddanddanserver.domain.usecase.auth

import notbe.tmtm.ddanddanserver.domain.gateway.TokenGateway
import notbe.tmtm.ddanddanserver.domain.gateway.UserGateway
import notbe.tmtm.ddanddanserver.domain.model.User
import notbe.tmtm.ddanddanserver.domain.usecase.UseCase
import org.springframework.stereotype.Component

@Component
class ReissueToken(
    private val userGateway: UserGateway,
    private val tokenGateway: TokenGateway,
) : UseCase<ReissueToken.ReissueTokenInput, ReissueToken.ReissueTokenOutput> {
    data class ReissueTokenInput(
        val refreshToken: String,
    )

    data class ReissueTokenOutput(
        val accessToken: String,
        val refreshToken: String,
        val user: User,
    )

    override fun execute(input: ReissueTokenInput): ReissueTokenOutput {
        tokenGateway.validateRefreshToken(refreshToken = input.refreshToken)

        val userId = tokenGateway.getUserIdFromToken(token = input.refreshToken)
        val user = userGateway.getById(userId)

        return ReissueTokenOutput(
            accessToken = tokenGateway.createAccessToken(user),
            refreshToken = tokenGateway.createRefreshToken(user),
            user = user,
        )
    }
}
