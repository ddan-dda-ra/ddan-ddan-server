package notbe.tmtm.ddanddanserver.infrastructure.client

import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class OAuthClientFactory(
    private val clients: List<OAuthClient>,
) {
    private lateinit var clientMap: Map<OAuthType, OAuthClient>

    @PostConstruct
    fun init() {
        clientMap = clients.associateBy { it.getProviderType() }
    }

    fun getClient(type: OAuthType): OAuthClient {
        clientMap[type]?.let {
            return it
        } ?: throw IllegalArgumentException("Unknown OAuth type: $type")
    }
}
