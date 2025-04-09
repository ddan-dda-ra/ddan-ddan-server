package notbe.tmtm.ddanddanserver.infrastructure.api

import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

@HttpExchange(url = "https://appleid.apple.com")
interface AppleAuthApi {
    @GetExchange(
        url = "/auth/keys",
        accept = [APPLICATION_JSON_VALUE],
    )
    fun getPublicKey(): ApplePublicKeyResponse

    data class ApplePublicKeyResponse(
        val keys: List<KeyResponse>,
    ) {
        data class KeyResponse(
            val kty: String,
            val kid: String,
            val use: String,
            val alg: String,
            val n: String,
            val e: String,
        )
    }
}
