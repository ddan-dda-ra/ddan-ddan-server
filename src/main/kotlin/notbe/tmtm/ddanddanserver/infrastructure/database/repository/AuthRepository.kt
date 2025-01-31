package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.AuthEntity
import org.springframework.data.mongodb.repository.MongoRepository

interface AuthRepository : MongoRepository<AuthEntity, String> {
    fun findByOAuthIdAndType(
        oAuthId: String,
        type: OAuthType,
    ): AuthEntity?

    fun deleteByUserId(userId: String)
}
