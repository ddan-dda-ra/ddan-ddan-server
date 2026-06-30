package notbe.tmtm.ddanddanserver.domain.model.petcatalog

import java.time.Instant

data class PetCatalog(
    val version: Instant,
    val pets: List<PetCatalogItem>,
)

data class PetCatalogItem(
    val type: String,
    val name: String,
    val colorCode: String,
    val isActive: Boolean,
    val displayOrder: Int,
    val levels: Map<Int, PetCatalogLevel>,
)

data class PetCatalogLevel(
    val imageUrl: String,
    val lottieDefaultUrl: String,
    val lottiePlayEatUrl: String,
)
