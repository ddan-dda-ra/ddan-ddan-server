package notbe.tmtm.ddanddanserver.infrastructure.gateway

import notbe.tmtm.ddanddanserver.domain.gateway.AuthGateway
import notbe.tmtm.ddanddanserver.domain.model.auth.Auth
import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.AuthEntity
import notbe.tmtm.ddanddanserver.infrastructure.database.respository.AuthRepository
import org.springframework.stereotype.Component

@Component
class AuthGatewayImpl(
    private val authRepository: AuthRepository,
) : AuthGateway {
    override fun save(auth: Auth): Auth {
        val authEntity = AuthEntity.fromDomain(auth)
        return authRepository.save(authEntity).toDomain()
    }

    override fun findByOAuthIdAndType(
        oAuthId: String,
        type: OAuthType,
    ): Auth? = authRepository.findByOAuthIdAndType(oAuthId, type)?.toDomain()
}
