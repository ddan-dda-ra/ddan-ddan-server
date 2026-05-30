package notbe.tmtm.ddanddanserver.application.processor

import notbe.tmtm.ddanddanserver.domain.exception.UnsupportedOAuthModeException
import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class OAuthProcessorFactory(
    private val clients: List<OAuthProcessor>,
) {
    private lateinit var realMap: Map<OAuthType, OAuthProcessor>
    private lateinit var mockMap: Map<OAuthType, OAuthProcessor>

    @PostConstruct
    fun init() {
        val (mocks, reals) = clients.partition { it is MockOAuthProcessor }
        realMap = reals.associateBy { it.getProviderType() }
        mockMap = mocks.associateBy { it.getProviderType() }
    }

    fun getClient(
        type: OAuthType,
        useMock: Boolean,
    ): OAuthProcessor {
        val pool = if (useMock) mockMap else realMap
        return pool[type] ?: run {
            if (useMock) {
                throw UnsupportedOAuthModeException(type)
            }
            throw IllegalArgumentException("Unknown OAuth type: $type")
        }
    }
}
