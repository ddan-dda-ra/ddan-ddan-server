package notbe.tmtm.ddanddanserver.domain.usecase.auth

import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType

data class OAuth(
    val id: String,
    val type: OAuthType,
    val nickName: String?,
)
