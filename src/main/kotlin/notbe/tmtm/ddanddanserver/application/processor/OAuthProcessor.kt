package notbe.tmtm.ddanddanserver.application.processor

import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType

interface OAuthProcessor {
    fun getOAuth(accessToken: String): OAuth

    fun getProviderType(): OAuthType
}
