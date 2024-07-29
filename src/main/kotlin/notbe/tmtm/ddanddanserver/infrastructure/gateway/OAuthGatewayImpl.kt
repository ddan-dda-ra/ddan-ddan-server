package notbe.tmtm.ddanddanserver.infrastructure.gateway

import notbe.tmtm.ddanddanserver.domain.gateway.OAuthGateway
import notbe.tmtm.ddanddanserver.infrastructure.client.KakaoClient
import org.springframework.stereotype.Component

@Component
class OAuthGatewayImpl(
    private val kakaoClient: KakaoClient,
) : OAuthGateway {
    override fun getOAuthUserInfo(accessToken: String) = kakaoClient.getOAuthInfo(accessToken).toDomain()
}
