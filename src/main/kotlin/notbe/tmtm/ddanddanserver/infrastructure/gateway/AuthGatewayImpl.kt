package notbe.tmtm.ddanddanserver.infrastructure.gateway

import notbe.tmtm.ddanddanserver.domain.gateway.AuthGateway
import notbe.tmtm.ddanddanserver.domain.model.auth.Auth
import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.AuthRepository
import org.bson.types.ObjectId
import org.springframework.stereotype.Component

@Component
class AuthGatewayImpl(
    private val authRepository: AuthRepository,
) : AuthGateway {
    override fun save(auth: Auth): Auth = authRepository.save(auth)

    override fun findByOAuthIdAndType(
        oAuthId: String,
        type: OAuthType,
    ): Auth? = authRepository.findByOAuthIdAndType(oAuthId, type)

    override fun deleteByUserId(userId: ObjectId) = authRepository.deleteByUserId(userId)
}
