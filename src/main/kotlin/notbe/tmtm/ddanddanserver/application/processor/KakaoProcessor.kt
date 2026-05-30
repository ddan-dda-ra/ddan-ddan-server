package notbe.tmtm.ddanddanserver.application.processor

import notbe.tmtm.ddanddanserver.common.util.logger
import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType
import notbe.tmtm.ddanddanserver.domain.exception.KakaoParseError
import notbe.tmtm.ddanddanserver.domain.exception.KakaoRestClientError
import notbe.tmtm.ddanddanserver.domain.exception.KakaoUnauthorizedError
import notbe.tmtm.ddanddanserver.infrastructure.api.KakaoAuthApi
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException

@Component
class KakaoProcessor(
    private val kakaoAuthApi: KakaoAuthApi,
) : OAuthProcessor {
    override fun getProviderType(): OAuthType = OAuthType.KAKAO

    override fun getOAuth(accessToken: String): OAuth =
        try {
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
                        nickName = response.properties?.nickname ?: response.id,
                    )
                }
        } catch (exception: KakaoParseError) {
            throw exception
        } catch (clientException: HttpClientErrorException) {
            throw KakaoUnauthorizedError()
        } catch (exception: Exception) {
            logger().error("Kakao API 요청 중 오류가 발생했습니다", exception)
            throw KakaoRestClientError(exception)
        }
}
