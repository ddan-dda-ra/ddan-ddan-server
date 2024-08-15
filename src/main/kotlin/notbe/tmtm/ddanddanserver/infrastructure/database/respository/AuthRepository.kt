package notbe.tmtm.ddanddanserver.infrastructure.database.respository

import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.AuthEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AuthRepository : JpaRepository<AuthEntity, String> {
    fun findByOAuthIdAndType(
        oAuthId: String,
        type: OAuthType,
    ): AuthEntity?
}
