package notbe.tmtm.ddanddanserver.presentation.dto.response

import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalog
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogBackgrounds
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogItem
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogLevel

data class PetCatalogResponse(
    val version: String,
    val pets: List<PetCatalogItemResponse>,
) {
    companion object {
        fun from(catalog: PetCatalog): PetCatalogResponse =
            PetCatalogResponse(
                version = catalog.version.toString(),
                pets = catalog.pets.map { PetCatalogItemResponse.from(it) },
            )
    }
}

data class PetCatalogItemResponse(
    val type: String,
    val name: String,
    val backgrounds: PetCatalogBackgroundsResponse,
    val isActive: Boolean,
    val displayOrder: Int,
    val levels: Map<Int, PetCatalogLevelResponse>,
) {
    companion object {
        fun from(item: PetCatalogItem): PetCatalogItemResponse =
            PetCatalogItemResponse(
                type = item.type,
                name = item.name,
                backgrounds = PetCatalogBackgroundsResponse.from(item.backgrounds),
                isActive = item.isActive,
                displayOrder = item.displayOrder,
                levels = item.levels.mapValues { PetCatalogLevelResponse.from(it.value) },
            )
    }
}

data class PetCatalogBackgroundsResponse(
    val home: String,
    val homeCompact: String,
    val friendCard: String,
) {
    companion object {
        fun from(backgrounds: PetCatalogBackgrounds): PetCatalogBackgroundsResponse =
            PetCatalogBackgroundsResponse(
                home = backgrounds.home,
                homeCompact = backgrounds.homeCompact,
                friendCard = backgrounds.friendCard,
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
