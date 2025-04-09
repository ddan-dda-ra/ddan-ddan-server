package notbe.tmtm.ddanddanserver.infrastructure.client

import notbe.tmtm.ddanddanserver.common.util.logger
import notbe.tmtm.ddanddanserver.domain.exception.KakaoParseError
import notbe.tmtm.ddanddanserver.domain.usecase.auth.OAuth
import notbe.tmtm.ddanddanserver.infrastructure.api.KakaoAuthApi
import org.springframework.stereotype.Component

@Component
class KakaoClient(
    private val kakaoAuthApi: KakaoAuthApi,
) : OAuthClient {
    override fun getOAuth(accessToken: String): OAuth =
        kakaoAuthApi
            .getUserInfo("Bearer $accessToken")
            .let { response ->
                if (response.id.isBlank()) {
                    logger().error("Kakao id is blank")
                    throw KakaoParseError()
                }
                OAuth(
                    id = response.id,
                    type = this.getProviderType(),
                    nickName = response.properties?.nickname,
                )
            }
}
