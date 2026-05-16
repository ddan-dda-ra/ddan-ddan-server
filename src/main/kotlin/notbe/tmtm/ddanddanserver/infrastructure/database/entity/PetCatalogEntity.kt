package notbe.tmtm.ddanddanserver.infrastructure.database.entity

import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalog
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogBackgrounds
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogItem
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogLevel
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("pet_catalog")
data class PetCatalogEntity(
    @Id
    val id: ObjectId = ObjectId(),
    @Indexed(unique = true)
    val type: String,
    val name: String,
    val backgrounds: PetCatalogBackgroundsEntity,
    val colorCode: String,
    val isActive: Boolean = true,
    val displayOrder: Int = 0,
    val levels: Map<Int, PetCatalogLevelEntity>,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    fun toDomain(): PetCatalogItem =
        PetCatalogItem(
            type = type,
            name = name,
            backgrounds = backgrounds.toDomain(),
            colorCode = colorCode,
            isActive = isActive,
            displayOrder = displayOrder,
            levels = levels.mapValues { it.value.toDomain() },
        )

    companion object {
        fun fromDomain(item: PetCatalogItem): PetCatalogEntity =
            PetCatalogEntity(
                type = item.type,
                name = item.name,
                backgrounds = PetCatalogBackgroundsEntity.fromDomain(item.backgrounds),
                colorCode = item.colorCode,
                isActive = item.isActive,
                displayOrder = item.displayOrder,
                levels = item.levels.mapValues { PetCatalogLevelEntity.fromDomain(it.value) },
            )
    }
}

data class PetCatalogBackgroundsEntity(
    val homeUrl: String,
    val homeCompactUrl: String,
    val friendCardUrl: String,
) {
    fun toDomain(): PetCatalogBackgrounds =
        PetCatalogBackgrounds(
            homeUrl = homeUrl,
            homeCompactUrl = homeCompactUrl,
            friendCardUrl = friendCardUrl,
        )

    companion object {
        fun fromDomain(backgrounds: PetCatalogBackgrounds): PetCatalogBackgroundsEntity =
            PetCatalogBackgroundsEntity(
                homeUrl = backgrounds.homeUrl,
                homeCompactUrl = backgrounds.homeCompactUrl,
                friendCardUrl = backgrounds.friendCardUrl,
            )
    }
}

data class PetCatalogLevelEntity(
    val imageUrl: String,
    val lottieDefaultUrl: String,
    val lottiePlayEatUrl: String,
) {
    fun toDomain(): PetCatalogLevel =
        PetCatalogLevel(
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

fun List<PetCatalogEntity>.toDomain(): PetCatalog =
    PetCatalog(
        version = this.maxOfOrNull { it.updatedAt } ?: Instant.EPOCH,
        pets = this.map { it.toDomain() },
    )
