package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalog
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.toDomain
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
        val entities = repository.findAllByIsActiveTrueOrderByDisplayOrderAsc()
        return entities.toDomain()
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
        versionCache.set(CachedVersion(version = fresh, cachedAt = now))
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
