package notbe.tmtm.ddanddanserver.infrastructure.database.entity

import notbe.tmtm.ddanddanserver.domain.model.auth.Auth
import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("auths")
data class AuthEntity(
    @Id
    val id: String,
    val oAuthId: String,
    val type: OAuthType,
    val userId: String,
) {
    fun toDomain(): Auth =
        Auth(
            id = id,
            oAuthId = oAuthId,
            type = type,
            userId = userId,
        )

    companion object {
        fun fromDomain(auth: Auth) =
            with(auth) {
                AuthEntity(
                    id = id,
                    oAuthId = oAuthId,
                    type = type,
                    userId = userId,
                )
            }
    }
}
