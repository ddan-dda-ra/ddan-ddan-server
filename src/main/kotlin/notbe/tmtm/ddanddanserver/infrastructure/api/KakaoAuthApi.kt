package notbe.tmtm.ddanddanserver.infrastructure.api

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

@HttpExchange(url = "https://kapi.kakao.com")
interface KakaoAuthApi {
    @GetExchange(
        url = "/v2/user/me",
        accept = [APPLICATION_JSON_VALUE],
    )
    fun getUserInfo(
        @RequestHeader(HttpHeaders.AUTHORIZATION) authorization: String,
    ): KakaoUserInfoResponse

    data class KakaoUserInfoResponse(
        val id: String,
        val properties: Properties?,
    ) {
        data class Properties(
            val nickname: String?,
        )
    }
}
