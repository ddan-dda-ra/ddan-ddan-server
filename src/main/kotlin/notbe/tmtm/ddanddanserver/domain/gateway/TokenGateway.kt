package notbe.tmtm.ddanddanserver.domain.gateway

import notbe.tmtm.ddanddanserver.domain.model.user.User

interface TokenGateway {
    fun createAccessToken(user: User): String

    fun createRefreshToken(user: User): String

    fun validateRefreshToken(refreshToken: String)

    fun getUserIdFromToken(token: String): String
}
