package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.domain.exception.PetCatalogDuplicateKeyException
import notbe.tmtm.ddanddanserver.domain.exception.PetCatalogNotFoundException
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalog
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogItem
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogLevel
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.PetCatalogEntity
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.PetCatalogLevelEntity
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.PetCatalogRepository
import org.springframework.dao.DuplicateKeyException
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
        if (!versionCache.compareAndSet(cached, CachedVersion(version = fresh, cachedAt = now))) {
            return versionCache.get()?.version ?: fresh
        }
        return fresh
    }

    // --- Admin operations ---

    fun getAllForAdmin(): List<PetCatalogItem> = repository.findAllByOrderByDisplayOrderAsc().map { it.toDomain() }

    fun create(
        key: String,
        name: String,
        isActive: Boolean,
        displayOrder: Int,
        levels: Map<Int, PetCatalogLevel>,
    ): PetCatalogItem {
        val now = Instant.now()
        val saved =
            try {
                repository.save(
                    PetCatalogEntity(
                        key = key,
                        name = name,
                        isActive = isActive,
                        displayOrder = displayOrder,
                        levels = levels.mapValues { PetCatalogLevelEntity.fromDomain(it.value) },
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            } catch (e: DuplicateKeyException) {
                // DB의 unique index가 race를 차단 — 이를 도메인 예외로 변환
                throw PetCatalogDuplicateKeyException(key)
            }
        invalidateVersionCache()
        return saved.toDomain()
    }

    fun update(
        key: String,
        name: String,
        isActive: Boolean,
        displayOrder: Int,
        levels: Map<Int, PetCatalogLevel>,
    ): PetCatalogItem {
        val existing = repository.findByKey(key) ?: throw PetCatalogNotFoundException(key)
        val updated =
            repository.save(
                existing.copy(
                    name = name,
                    isActive = isActive,
                    displayOrder = displayOrder,
                    levels = levels.mapValues { PetCatalogLevelEntity.fromDomain(it.value) },
                    updatedAt = Instant.now(),
                ),
            )
        invalidateVersionCache()
        return updated.toDomain()
    }

    fun softDelete(key: String): PetCatalogItem {
        val existing = repository.findByKey(key) ?: throw PetCatalogNotFoundException(key)
        if (!existing.isActive) {
            return existing.toDomain()
        }
        val updated =
            repository.save(
                existing.copy(
                    isActive = false,
                    updatedAt = Instant.now(),
                ),
            )
        invalidateVersionCache()
        return updated.toDomain()
    }

    private fun invalidateVersionCache() {
        versionCache.set(null)
    }

    private data class CachedVersion(
        val version: Instant,
        val cachedAt: Instant,
    )

    companion object {
        private val CACHE_TTL: Duration = Duration.ofSeconds(60)
    }
}
