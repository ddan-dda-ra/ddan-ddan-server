package notbe.tmtm.ddanddanserver.presentation.dto.response

import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalog
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogItem
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogLevel

data class PetCatalogResponse(
    val revision: String,
    val pets: List<PetCatalogItemResponse>,
) {
    companion object {
        fun from(catalog: PetCatalog) =
            PetCatalogResponse(
                revision = catalog.revision.toString(),
                pets = catalog.pets.map(PetCatalogItemResponse::from),
            )
    }
}

data class PetCatalogItemResponse(
    val type: String,
    val name: String,
    val colorCode: String,
    val displayOrder: Int,
    val levels: List<PetCatalogLevelResponse>,
) {
    companion object {
        fun from(item: PetCatalogItem) =
            PetCatalogItemResponse(
                item.type,
                item.name,
                item.colorCode,
                item.displayOrder,
                item.levels.map(PetCatalogLevelResponse::from),
            )
    }
}

data class PetCatalogLevelResponse(
    val level: Int,
    val imageUrl: String,
    val lottieDefaultUrl: String,
    val lottiePlayEatUrl: String,
) {
    companion object {
        fun from(level: PetCatalogLevel) =
            PetCatalogLevelResponse(level.level, level.imageUrl, level.lottieDefaultUrl, level.lottiePlayEatUrl)
    }
}
