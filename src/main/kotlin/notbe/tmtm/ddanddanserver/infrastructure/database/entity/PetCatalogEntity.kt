package notbe.tmtm.ddanddanserver.infrastructure.database.entity

import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogItem
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogLevel
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("pet_catalog")
@CompoundIndex(name = "is_active_updated_at_idx", def = "{'is_active': 1, 'updated_at': -1}")
data class PetCatalogEntity(
    @Id
    val id: ObjectId = ObjectId(),
    @Indexed(unique = true)
    val type: String,
    val name: String,
    val colorCode: String,
    val isActive: Boolean = true,
    val displayOrder: Int = 0,
    val levels: Map<Int, PetCatalogLevelEntity>,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    fun toDomain(): PetCatalogItem =
        PetCatalogItem.create(
            type = type,
            name = name,
            colorCode = colorCode,
            isActive = isActive,
            displayOrder = displayOrder,
            levels = levels.map { (level, value) -> value.toDomain(level) },
        )

    companion object {
        fun fromDomain(item: PetCatalogItem): PetCatalogEntity =
            PetCatalogEntity(
                type = item.type,
                name = item.name,
                colorCode = item.colorCode,
                isActive = item.isActive,
                displayOrder = item.displayOrder,
                levels = item.levels.associate { it.level to PetCatalogLevelEntity.fromDomain(it) },
            )
    }
}

data class PetCatalogLevelEntity(
    val imageUrl: String,
    val lottieDefaultUrl: String,
    val lottiePlayEatUrl: String,
) {
    fun toDomain(level: Int): PetCatalogLevel =
        PetCatalogLevel(
            level = level,
            imageUrl = imageUrl,
            lottieDefaultUrl = lottieDefaultUrl,
            lottiePlayEatUrl = lottiePlayEatUrl,
        )

    companion object {
        fun fromDomain(level: PetCatalogLevel): PetCatalogLevelEntity =
            PetCatalogLevelEntity(
                imageUrl = level.imageUrl,
                lottieDefaultUrl = level.lottieDefaultUrl,
                lottiePlayEatUrl = level.lottiePlayEatUrl,
            )
    }
}
