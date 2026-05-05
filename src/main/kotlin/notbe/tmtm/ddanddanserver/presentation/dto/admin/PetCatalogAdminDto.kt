package notbe.tmtm.ddanddanserver.presentation.dto.admin

import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogItem
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogLevel

data class PetCatalogAdminCreateRequest(
    val key: String,
    val name: String,
    val isActive: Boolean = true,
    val displayOrder: Int = 0,
    val levels: Map<Int, PetCatalogLevelRequest>,
) {
    fun levelsToDomain(): Map<Int, PetCatalogLevel> = levels.mapValues { it.value.toDomain() }
}

data class PetCatalogAdminUpdateRequest(
    val name: String,
    val isActive: Boolean,
    val displayOrder: Int,
    val levels: Map<Int, PetCatalogLevelRequest>,
) {
    fun levelsToDomain(): Map<Int, PetCatalogLevel> = levels.mapValues { it.value.toDomain() }
}

data class PetCatalogLevelRequest(
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
}

data class PetCatalogAdminItemResponse(
    val key: String,
    val name: String,
    val isActive: Boolean,
    val displayOrder: Int,
    val levels: Map<Int, PetCatalogLevelResponse>,
) {
    companion object {
        fun from(item: PetCatalogItem): PetCatalogAdminItemResponse =
            PetCatalogAdminItemResponse(
                key = item.key,
                name = item.name,
                isActive = item.isActive,
                displayOrder = item.displayOrder,
                levels = item.levels.mapValues { PetCatalogLevelResponse.from(it.value) },
            )
    }
}

data class PetCatalogLevelResponse(
    val imageUrl: String,
    val lottieDefaultUrl: String,
    val lottiePlayEatUrl: String,
) {
    companion object {
        fun from(level: PetCatalogLevel): PetCatalogLevelResponse =
            PetCatalogLevelResponse(
                imageUrl = level.imageUrl,
                lottieDefaultUrl = level.lottieDefaultUrl,
                lottiePlayEatUrl = level.lottiePlayEatUrl,
            )
    }
}

data class PetCatalogAdminListResponse(
    val pets: List<PetCatalogAdminItemResponse>,
) {
    companion object {
        fun from(items: List<PetCatalogItem>): PetCatalogAdminListResponse =
            PetCatalogAdminListResponse(pets = items.map { PetCatalogAdminItemResponse.from(it) })
    }
}
