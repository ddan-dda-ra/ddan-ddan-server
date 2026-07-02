package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.domain.exception.PetCatalogInactiveException
import notbe.tmtm.ddanddanserver.domain.exception.PetCatalogNotFoundException
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalog
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogItem
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.PetCatalogRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class PetCatalogService(
    private val repository: PetCatalogRepository,
) {
    fun currentRevision(): Instant = repository.findTopByIsActiveTrueOrderByUpdatedAtDesc()?.updatedAt ?: Instant.EPOCH

    fun getCatalog(): PetCatalog {
        val entities = repository.findAllByIsActiveTrueOrderByDisplayOrderAscTypeAsc()
        return PetCatalog(
            revision = entities.maxOfOrNull { it.updatedAt } ?: Instant.EPOCH,
            pets = entities.map { it.toDomain() },
        )
    }

    fun pickRandomActiveExcluding(excludedTypes: List<String>): String {
        val active = repository.findAllByIsActiveTrueOrderByDisplayOrderAscTypeAsc().map { it.type }
        require(active.isNotEmpty()) { "no active pet species" }
        val canonicalExcluded = excludedTypes.map(PetCatalogItem::canonicalizeType).toSet()
        return active.filterNot { it in canonicalExcluded }.ifEmpty { active }.random()
    }

    fun getName(type: String): String? = repository.findByType(PetCatalogItem.canonicalizeType(type))?.name

    fun requireActive(type: String) {
        val canonicalType = PetCatalogItem.canonicalizeType(type)
        val entity = repository.findByType(canonicalType) ?: throw PetCatalogNotFoundException(canonicalType)
        if (!entity.isActive) throw PetCatalogInactiveException(canonicalType)
    }
}
