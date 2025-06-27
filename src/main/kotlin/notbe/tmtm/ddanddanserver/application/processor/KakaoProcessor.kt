package notbe.tmtm.ddanddanserver.application.processor

import notbe.tmtm.ddanddanserver.common.util.logger
import notbe.tmtm.ddanddanserver.domain.exception.KakaoParseError
import notbe.tmtm.ddanddanserver.infrastructure.api.KakaoAuthApi
import org.springframework.stereotype.Component

@Component
class KakaoProcessor(
    private val kakaoAuthApi: KakaoAuthApi,
) : OAuthProcessor {
    override fun getOAuth(accessToken: String): OAuthInfo =
        kakaoAuthApi
            .getUserInfo("Bearer $accessToken")
            .let { response ->
                if (response.id.isBlank()) {
                    logger().error("Kakao id is blank")
                    throw KakaoParseError()
                }
                OAuthInfo(
                    id = response.id,
                    type = this.getProviderType(),
                    nickName = response.properties?.nickname,
                )
            }
}
