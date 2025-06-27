package notbe.tmtm.ddanddanserver.infrastructure.gateway

import notbe.tmtm.ddanddanserver.common.JWTTokenProvider
import notbe.tmtm.ddanddanserver.domain.model.user.User
import org.bson.types.ObjectId
import org.springframework.stereotype.Component

@Component
class TokenAdapter(
    private val jwtTokenProvider: JWTTokenProvider,
) {
    fun createAccessToken(user: User) = jwtTokenProvider.createAccessToken(user)

    fun createRefreshToken(user: User) = jwtTokenProvider.createRefreshToken(user)

    fun validateRefreshToken(refreshToken: String) = jwtTokenProvider.validateRefreshToken(refreshToken)

    fun getUserIdFromToken(token: String): ObjectId = jwtTokenProvider.getUserIdFromRefreshToken(token)
}
