package notbe.tmtm.ddanddanserver.infrastructure.client

import notbe.tmtm.ddanddanserver.common.util.logger
import notbe.tmtm.ddanddanserver.domain.exception.KakaoParseError
import notbe.tmtm.ddanddanserver.domain.exception.KakaoRestClientError
import notbe.tmtm.ddanddanserver.domain.usecase.auth.OAuth
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.URI

@Component
class KakaoClient(
    private val restClient: RestClient,
) : OAuthClient {
    private val baseUrl: String = "https://kapi.kakao.com"

    override fun getOAuth(accessToken: String): OAuth =
        getKakaoUserInfo(accessToken).let {
            OAuth(
                id = it.id,
                type = this.getProviderType(),
                nickName = it.properties?.nickname,
            )
        }

    fun getKakaoUserInfo(token: String): KakaoOAuthInfoResponse =
        restClient
            .get()
            .uri(URI.create("$baseUrl/v2/user/me"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(HttpStatusCode::isError) { _, response ->
                logger().error(
                    "Kakao API error: ${
                        response.body.use {
                            it.readAllBytes().toString(Charsets.UTF_8)
                        }
                    }",
                )
                throw KakaoRestClientError(response.statusCode)
            }.toEntity(KakaoOAuthInfoResponse::class.java)
            .body ?: throw KakaoParseError()

    data class KakaoOAuthInfoResponse(
        val id: String,
        val properties: Properties?,
    ) {
        data class Properties(
            val nickname: String?,
        )
    }
}
