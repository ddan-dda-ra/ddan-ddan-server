package notbe.tmtm.ddanddanserver.infrastructure.gateway

import notbe.tmtm.ddanddanserver.domain.model.auth.Auth
import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.AuthRepository
import org.springframework.stereotype.Component

@Component
class AuthAdapter(
    private val authRepository: AuthRepository,
) {
    fun save(auth: Auth): Auth = authRepository.save(auth)

    fun findByOAuthIdAndType(
        oAuthId: String,
        type: OAuthType,
    ): Auth? = authRepository.findByOAuthIdAndType(oAuthId, type)
}
