package notbe.tmtm.ddanddanserver.infrastructure.database.repository

import notbe.tmtm.ddanddanserver.domain.model.auth.Auth
import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface AuthRepository : MongoRepository<Auth, ObjectId> {
    fun findByOAuthIdAndType(
        oAuthId: String,
        type: OAuthType,
    ): Auth?

    fun deleteAllByUserId(userId: ObjectId)
}
