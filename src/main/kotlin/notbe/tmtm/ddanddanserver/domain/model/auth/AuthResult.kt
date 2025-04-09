package notbe.tmtm.ddanddanserver.domain.model.auth

import notbe.tmtm.ddanddanserver.domain.model.user.User

data class AuthResult(
    val accessToken: String,
    val refreshToken: String,
    val user: User,
)
