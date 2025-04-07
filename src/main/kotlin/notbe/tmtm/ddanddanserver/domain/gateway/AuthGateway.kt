package notbe.tmtm.ddanddanserver.domain.gateway

import notbe.tmtm.ddanddanserver.domain.model.auth.Auth
import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType
import org.bson.types.ObjectId

interface AuthGateway {
    fun save(auth: Auth): Auth

    fun findByOAuthIdAndType(
        oAuthId: String,
        type: OAuthType,
    ): Auth?

    fun deleteByUserId(userId: ObjectId)
}
