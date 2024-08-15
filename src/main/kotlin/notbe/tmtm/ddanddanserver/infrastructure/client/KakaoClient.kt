package notbe.tmtm.ddanddanserver.infrastructure.client

import notbe.tmtm.ddanddanserver.domain.exception.KakaoParseError
import notbe.tmtm.ddanddanserver.domain.exception.KakaoRestClientError
import notbe.tmtm.ddanddanserver.infrastructure.client.dto.response.KakaoOAuthInfoResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.URI

@Component
class KakaoClient(
    private val restClient: RestClient,
) {
    private val baseUrl: String = "https://kapi.kakao.com"

    fun getOAuthInfo(token: String) =
        runCatching {
            restClient
                .get()
                .uri(URI.create("$baseUrl/v2/user/me"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError) { _, response ->
                    throw KakaoRestClientError(response.statusCode)
                }.toEntity(KakaoOAuthInfoResponse::class.java)
        }.getOrElse { throw KakaoParseError(it) }
}
