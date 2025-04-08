package notbe.tmtm.ddanddanserver.infrastructure.client

import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType
import notbe.tmtm.ddanddanserver.domain.usecase.auth.OAuth

interface OAuthClient {
    fun getOAuth(accessToken: String): OAuth

    fun getProviderType(): OAuthType =
        when (this) {
            is KakaoClient -> OAuthType.KAKAO
            is AppleClient -> OAuthType.APPLE
            else -> throw IllegalArgumentException("Unknown OAuth client")
        }
}
