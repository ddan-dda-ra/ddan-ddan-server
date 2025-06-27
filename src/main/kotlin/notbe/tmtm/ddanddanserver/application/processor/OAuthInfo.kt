package notbe.tmtm.ddanddanserver.application.processor

import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType

data class OAuthInfo(
    val id: String,
    val type: OAuthType,
    val nickName: String?,
)
