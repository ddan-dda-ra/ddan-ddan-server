package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalog
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.PetCatalogEntity
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.PetCatalogRepository
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

@Service
class PetCatalogService(
    private val repository: PetCatalogRepository,
) {
    private val versionCache = AtomicReference<CachedVersion?>()

    fun getActiveCatalog(): PetCatalog {
        val entities: List<PetCatalogEntity> = repository.findAllByIsActiveTrueOrderByDisplayOrderAsc()
        return PetCatalog(
            version = currentVersion(),
            pets = entities.map { it.toDomain() },
        )
    }

    fun currentVersion(): Instant {
        val now = Instant.now()
        val cached = versionCache.get()
        if (cached != null && Duration.between(cached.cachedAt, now) < CACHE_TTL) {
            return cached.version
        }
        val fresh =
            repository.findAll().maxOfOrNull { it.updatedAt }
                ?: Instant.EPOCH
        // CAS — TTL 만료 직후 다수 스레드가 동시에 fresh를 쓰는 race를 줄여 한 번만 갱신
        if (!versionCache.compareAndSet(cached, CachedVersion(version = fresh, cachedAt = now))) {
            // 다른 스레드가 먼저 갱신 — 그 결과를 사용
            return versionCache.get()?.version ?: fresh
        }
        return fresh
    }

    private data class CachedVersion(
        val version: Instant,
        val cachedAt: Instant,
    )

    companion object {
        private val CACHE_TTL: Duration = Duration.ofSeconds(60)
    }
}
