package notbe.tmtm.ddanddanserver.presentation.dto.admin

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogBackgrounds
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogItem
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogLevel

data class PetCatalogAdminCreateRequest(
    @field:NotBlank
    val type: String,
    @field:NotBlank
    val name: String,
    @field:Valid
    val backgrounds: PetCatalogBackgroundsRequest,
    val isActive: Boolean = true,
    @field:Min(0)
    val displayOrder: Int = 0,
    @field:NotEmpty
    @field:Valid
    val levels: Map<Int, PetCatalogLevelRequest>,
) {
    fun levelsToDomain(): Map<Int, PetCatalogLevel> = levels.mapValues { it.value.toDomain() }
}

data class PetCatalogAdminUpdateRequest(
    @field:NotBlank
    val name: String,
    @field:Valid
    val backgrounds: PetCatalogBackgroundsRequest,
    val isActive: Boolean,
    @field:Min(0)
    val displayOrder: Int,
    @field:NotEmpty
    @field:Valid
    val levels: Map<Int, PetCatalogLevelRequest>,
) {
    fun levelsToDomain(): Map<Int, PetCatalogLevel> = levels.mapValues { it.value.toDomain() }
}

data class PetCatalogBackgroundsRequest(
    @field:NotBlank
    val homeUrl: String,
    @field:NotBlank
    val homeCompactUrl: String,
    @field:NotBlank
    val friendCardUrl: String,
) {
    fun toDomain(): PetCatalogBackgrounds =
        PetCatalogBackgrounds(
            homeUrl = homeUrl,
            homeCompactUrl = homeCompactUrl,
            friendCardUrl = friendCardUrl,
        )
}

data class PetCatalogLevelRequest(
    @field:NotBlank
    val imageUrl: String,
    @field:NotBlank
    val lottieDefaultUrl: String,
    @field:NotBlank
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
    val type: String,
    val name: String,
    val backgrounds: PetCatalogBackgroundsResponse,
    val isActive: Boolean,
    val displayOrder: Int,
    val levels: Map<Int, PetCatalogLevelResponse>,
) {
    companion object {
        fun from(item: PetCatalogItem): PetCatalogAdminItemResponse =
            PetCatalogAdminItemResponse(
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
    val homeUrl: String,
    val homeCompactUrl: String,
    val friendCardUrl: String,
) {
    companion object {
        fun from(backgrounds: PetCatalogBackgrounds): PetCatalogBackgroundsResponse =
            PetCatalogBackgroundsResponse(
                homeUrl = backgrounds.homeUrl,
                homeCompactUrl = backgrounds.homeCompactUrl,
                friendCardUrl = backgrounds.friendCardUrl,
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
