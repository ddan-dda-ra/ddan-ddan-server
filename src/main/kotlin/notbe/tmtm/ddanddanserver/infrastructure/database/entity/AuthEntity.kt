package notbe.tmtm.ddanddanserver.infrastructure.database.entity

import jakarta.persistence.*
import notbe.tmtm.ddanddanserver.domain.model.auth.Auth
import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType

@Entity(name = "auths")
data class AuthEntity(
    @Id
    @Column(name = "id", length = 13, columnDefinition = "CHAR(13)", nullable = false)
    val id: String,
    @Column(name = "oauth_id", nullable = false)
    val oAuthId: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, columnDefinition = "VARCHAR(255)")
    val type: OAuthType,
    @Column(name = "user_id", nullable = false)
    val userId: String,
) : BaseEntity() {
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
