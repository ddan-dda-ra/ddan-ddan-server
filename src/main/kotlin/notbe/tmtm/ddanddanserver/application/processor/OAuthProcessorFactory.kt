package notbe.tmtm.ddanddanserver.application.processor

import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class OAuthProcessorFactory(
    private val clients: List<OAuthProcessor>,
) {
    private lateinit var clientMap: Map<OAuthType, OAuthProcessor>

    @PostConstruct
    fun init() {
        clientMap = clients.associateBy { it.getProviderType() }
    }

    fun getClient(type: OAuthType): OAuthProcessor {
        clientMap[type]?.let {
            return it
        } ?: throw IllegalArgumentException("Unknown OAuth type: $type")
    }
}
