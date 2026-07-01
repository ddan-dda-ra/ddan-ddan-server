package notbe.tmtm.ddanddanserver.application.service

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import notbe.tmtm.ddanddanserver.domain.exception.PetCatalogInvalidException
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogAssetUrl
import org.springframework.core.env.Environment

class PetCatalogAssetUrlPolicyTest : FunSpec({
    fun policy(profile: String, localHosts: List<String> = emptyList()): PetCatalogAssetUrlPolicy {
        val environment = mockk<Environment>()
        every { environment.activeProfiles } returns arrayOf(profile)
        return PetCatalogAssetUrlPolicy(environment, listOf("cdn.test"), localHosts)
    }

    test("prod는 allowlist 정확 host의 HTTPS 기본 포트와 443만 허용한다") {
        val policy = policy("prod")
        shouldNotThrow<Throwable> { policy.validate(PetCatalogAssetUrl.image("https://cdn.test/a.svg")) }
        shouldNotThrow<Throwable> { policy.validate(PetCatalogAssetUrl.image("https://cdn.test:443/a.svg")) }
        listOf("http://cdn.test/a.svg", "https://sub.cdn.test/a.svg", "https://cdn.test:8443/a.svg").forEach {
            shouldThrow<PetCatalogInvalidException> { policy.validate(PetCatalogAssetUrl.image(it)) }
        }
    }

    test("non-prod는 loopback과 명시한 개발 host의 HTTP만 추가 허용한다") {
        val policy = policy("test", listOf("assets.local"))
        listOf("http://localhost/a.svg", "http://127.0.0.1/a.png", "http://[::1]/a.webp", "http://assets.local/a.svg").forEach {
            shouldNotThrow<Throwable> { policy.validate(PetCatalogAssetUrl.image(it)) }
        }
        shouldThrow<PetCatalogInvalidException> { policy.validate(PetCatalogAssetUrl.image("http://192.168.0.2/a.svg")) }
    }
})
