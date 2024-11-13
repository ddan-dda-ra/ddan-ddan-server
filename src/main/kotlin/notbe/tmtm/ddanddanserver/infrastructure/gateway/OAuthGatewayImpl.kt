package notbe.tmtm.ddanddanserver.infrastructure.gateway

import notbe.tmtm.ddanddanserver.domain.gateway.OAuthGateway
import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthInfo
import notbe.tmtm.ddanddanserver.infrastructure.client.AppleClient
import notbe.tmtm.ddanddanserver.infrastructure.client.KakaoClient
import org.springframework.stereotype.Component

@Component
class OAuthGatewayImpl(
    private val kakaoClient: KakaoClient,
    private val appleClient: AppleClient,
) : OAuthGateway {
    override fun getOAuthUserInfo(accessToken: String) = kakaoClient.getOAuthInfo(accessToken).toDomain()

    override fun getOAuthUserInfoFromApple(accessToken: String): OAuthInfo = appleClient.getOAuthInfo(accessToken).toDomain()
}
