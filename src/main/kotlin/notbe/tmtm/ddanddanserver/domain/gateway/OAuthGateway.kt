package notbe.tmtm.ddanddanserver.domain.gateway

import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthInfo

interface OAuthGateway {
    fun getOAuthUserInfo(accessToken: String): OAuthInfo
}
