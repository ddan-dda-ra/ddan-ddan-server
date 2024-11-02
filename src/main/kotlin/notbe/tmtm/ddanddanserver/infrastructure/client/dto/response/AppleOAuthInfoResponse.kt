package notbe.tmtm.ddanddanserver.infrastructure.client.dto.response

import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthInfo
import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType

data class AppleOAuthInfoResponse(
    val id: String,
    val properties: Properties,
) {
    data class Properties(
        val nickname: String?,
    )

    fun toDomain() =
        OAuthInfo(
            id = id,
            type = OAuthType.APPLE,
            nickName = properties.nickname,
        )
}
