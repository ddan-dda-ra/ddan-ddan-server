package notbe.tmtm.ddanddanserver.application.service

import notbe.tmtm.ddanddanserver.domain.exception.PetCatalogDuplicateKeyException
import notbe.tmtm.ddanddanserver.domain.exception.PetCatalogInactiveException
import notbe.tmtm.ddanddanserver.domain.exception.PetCatalogInvalidException
import notbe.tmtm.ddanddanserver.domain.exception.PetCatalogNotFoundException
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalog
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogItem
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogLevel
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.PetCatalogEntity
import notbe.tmtm.ddanddanserver.infrastructure.database.entity.PetCatalogLevelEntity
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.PetCatalogRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import java.time.Instant

data class UpsertPetCatalogCommand(
    val type: String,
    val name: String,
    val colorCode: String,
    val isActive: Boolean,
    val displayOrder: Int,
    val levels: List<PetCatalogLevel>,
)

@Service
class PetCatalogService(
    private val repository: PetCatalogRepository,
    private val assetUrlPolicy: PetCatalogAssetUrlPolicy,
) {
    fun getCatalog(): PetCatalog {
        val entities = repository.findAllByIsActiveTrueOrderByDisplayOrderAscTypeAsc()
        return PetCatalog(
            revision = entities.maxOfOrNull { it.updatedAt } ?: Instant.EPOCH,
            pets = entities.map { it.toDomain() },
        )
    }

    fun getAllForAdmin(): List<PetCatalogItem> =
        repository.findAllByOrderByDisplayOrderAscTypeAsc().map { it.toDomain() }

    fun create(command: UpsertPetCatalogCommand): PetCatalogItem {
        val item = command.toDomain()
        val now = Instant.now()
        val entity =
            PetCatalogEntity(
                type = item.type,
                name = item.name,
                colorCode = item.colorCode,
                isActive = item.isActive,
                displayOrder = item.displayOrder,
                levels = item.levels.associate { it.level to PetCatalogLevelEntity.fromDomain(it) },
                createdAt = now,
                updatedAt = now,
            )
        return try {
            repository.save(entity).toDomain()
        } catch (exception: DuplicateKeyException) {
            throw PetCatalogDuplicateKeyException(item.type)
        }
    }

    fun update(
        pathType: String,
        command: UpsertPetCatalogCommand,
    ): PetCatalogItem {
        val canonicalPathType = PetCatalogItem.canonicalizeType(pathType)
        val existing = repository.findByType(canonicalPathType) ?: throw PetCatalogNotFoundException(canonicalPathType)
        if (PetCatalogItem.canonicalizeType(command.type) != canonicalPathType) {
            throw PetCatalogInvalidException("path type은 변경할 수 없습니다")
        }
        if (existing.isActive && !command.isActive) {
            throw PetCatalogInvalidException("활성 카탈로그는 비활성화할 수 없습니다")
        }
        val item = command.toDomain()
        return repository.save(
            existing.copy(
                name = item.name,
                colorCode = item.colorCode,
                isActive = item.isActive,
                displayOrder = item.displayOrder,
                levels = item.levels.associate { it.level to PetCatalogLevelEntity.fromDomain(it) },
                updatedAt = Instant.now(),
            ),
        ).toDomain()
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

    private fun UpsertPetCatalogCommand.toDomain(): PetCatalogItem =
        PetCatalogItem.create(type, name, colorCode, isActive, displayOrder, levels, assetUrlPolicy::validate)
}
