package notbe.tmtm.ddanddanserver.infrastructure.gateway

import notbe.tmtm.ddanddanserver.domain.gateway.TokenGateway
import notbe.tmtm.ddanddanserver.domain.model.User
import notbe.tmtm.ddanddanserver.infrastructure.token.JWTTokenProvider
import org.springframework.stereotype.Component

@Component
class TokenGatewayImpl(
    private val jwtTokenProvider: JWTTokenProvider,
) : TokenGateway {
    override fun createAccessToken(user: User) = jwtTokenProvider.createAccessToken(user)

    override fun createRefreshToken(user: User) = jwtTokenProvider.createRefreshToken(user)

    override fun validateRefreshToken(refreshToken: String) = jwtTokenProvider.validateRefreshToken(refreshToken)

    override fun getUserIdFromToken(token: String) = jwtTokenProvider.getUserIdFromRefreshToken(token)
}
