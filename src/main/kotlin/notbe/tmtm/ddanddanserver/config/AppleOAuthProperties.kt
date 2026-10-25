package notbe.tmtm.ddanddanserver.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Apple 로그인 id_token 검증 설정.
 * id_token의 `aud`는 [clientIds] 중 하나와 일치해야 한다.
 */
@ConfigurationProperties(prefix = "oauth.apple")
data class AppleOAuthProperties(
    val clientIds: List<String> = listOf(DEFAULT_CLIENT_ID),
) {
    init {
        require(clientIds.isNotEmpty() && clientIds.none { it.isBlank() }) {
            "oauth.apple.client-ids 설정값이 비어 있습니다: $clientIds"
        }
    }

    companion object {
        const val DEFAULT_CLIENT_ID = "inc.ddanddan.DDanDDan"
    }
}
