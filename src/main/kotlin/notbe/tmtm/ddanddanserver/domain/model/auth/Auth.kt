package notbe.tmtm.ddanddanserver.domain.model.auth

import notbe.tmtm.ddanddanserver.common.util.generateTsid

class Auth(
    val id: String,
    val oAuthId: String,
    val type: OAuthType,
    val userId: String,
) {
    companion object {
        fun create(
            oAuthId: String,
            type: OAuthType,
            userId: String,
        ): Auth =
            Auth(
                id = generateTsid(),
                oAuthId = oAuthId,
                type = type,
                userId = userId,
            )
    }
}
