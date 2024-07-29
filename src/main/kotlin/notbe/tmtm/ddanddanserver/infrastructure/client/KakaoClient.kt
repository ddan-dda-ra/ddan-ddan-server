package notbe.tmtm.ddanddanserver.infrastructure.client

import notbe.tmtm.ddanddanserver.infrastructure.client.dto.response.KakaoOAuthInfoResponse
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.URI

@Component
class KakaoClient(
    private val restClient: RestClient,
) {
    fun getOAuthInfo(token: String): KakaoOAuthInfoResponse {
        val response =
            restClient
                .get()
                .uri(URI.create("https://kapi.kakao.com/v2/user/me"))
                .header("Authorization", "Bearer $token")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity(KakaoOAuthInfoResponse::class.java)

        return response.body ?: throw IllegalStateException("Failed to get Kakao auth info")
    }
}
