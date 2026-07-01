package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.domain.exception.PetCatalogInvalidException
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogAssetUrl
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import java.util.Locale

@Component
class PetCatalogAssetUrlPolicy(
    environment: Environment,
    @Value("\${pet-catalog.asset.allowed-hosts:ddan-ddan-cdn.ddmz.org}") allowedHosts: List<String>,
    @Value("\${pet-catalog.asset.local-http-hosts:}") configuredLocalHttpHosts: List<String>,
) {
    private val isProd = environment.activeProfiles.any { it == "prod" }
    private val allowedHosts = allowedHosts.map(::canonicalizeHost).toSet()
    private val localHttpHosts =
        configuredLocalHttpHosts.map(::canonicalizeHost).toSet() +
            setOf("localhost", "127.0.0.1", "::1")

    fun validate(assetUrl: PetCatalogAssetUrl) {
        val uri = assetUrl.uri
        val scheme = uri.scheme.lowercase(Locale.ROOT)
        val host = canonicalizeHost(uri.host)
        val validHttps = scheme == "https" && host in allowedHosts && (uri.port == -1 || uri.port == 443)
        val validLocalHttp = !isProd && scheme == "http" && host in localHttpHosts
        if (!validHttps && !validLocalHttp) {
            throw PetCatalogInvalidException("허용되지 않은 에셋 URL scheme, host 또는 port입니다")
        }
    }

    private fun canonicalizeHost(host: String): String =
        host.lowercase(Locale.ROOT).removePrefix("[").removeSuffix("]")
}
