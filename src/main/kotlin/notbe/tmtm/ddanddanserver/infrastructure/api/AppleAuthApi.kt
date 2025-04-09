package notbe.tmtm.ddanddanserver.infrastructure.api

import org.springframework.web.service.annotation.HttpExchange

@HttpExchange(url = "https://appleid.apple.com")
interface AppleAuthApi {
    @HttpExchange(url = "/auth/keys")
    fun getPublicKey(): ApplePublicKeyResponse

    data class ApplePublicKeyResponse(
        val key: List<KeyResponse>,
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
