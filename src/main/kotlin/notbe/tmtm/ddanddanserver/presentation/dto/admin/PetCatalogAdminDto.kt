package notbe.tmtm.ddanddanserver.presentation.dto.admin

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import notbe.tmtm.ddanddanserver.application.service.UpsertPetCatalogCommand
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogItem
import notbe.tmtm.ddanddanserver.domain.model.petcatalog.PetCatalogLevel

private const val HEX_COLOR_REGEX = "^#[0-9A-Fa-f]{6}$"

data class PetCatalogAdminCreateRequest(
    @field:NotBlank val type: String,
    @field:NotBlank val name: String,
    @field:Pattern(regexp = HEX_COLOR_REGEX) val colorCode: String,
    val isActive: Boolean = false,
    @field:Min(0) val displayOrder: Int = 0,
    @field:NotEmpty @field:Valid val levels: List<PetCatalogLevelRequest>,
) {
    fun toCommand() = UpsertPetCatalogCommand(type, name, colorCode, isActive, displayOrder, levels.map { it.toDomain() })
}

data class PetCatalogAdminUpdateRequest(
    @field:NotBlank val name: String,
    @field:Pattern(regexp = HEX_COLOR_REGEX) val colorCode: String,
    val isActive: Boolean,
    @field:Min(0) val displayOrder: Int,
    @field:NotEmpty @field:Valid val levels: List<PetCatalogLevelRequest>,
) {
    fun toCommand(type: String) = UpsertPetCatalogCommand(type, name, colorCode, isActive, displayOrder, levels.map { it.toDomain() })
}

data class PetCatalogLevelRequest(
    val level: Int,
    @field:NotBlank val imageUrl: String,
    @field:NotBlank val lottieDefaultUrl: String,
    @field:NotBlank val lottiePlayEatUrl: String,
) {
    fun toDomain() = PetCatalogLevel(level, imageUrl, lottieDefaultUrl, lottiePlayEatUrl)
}

data class PetCatalogAdminItemResponse(
    val type: String,
    val name: String,
    val colorCode: String,
    val isActive: Boolean,
    val displayOrder: Int,
    val levels: List<PetCatalogLevelResponse>,
) {
    companion object {
        fun from(item: PetCatalogItem) =
            PetCatalogAdminItemResponse(
                item.type,
                item.name,
                item.colorCode,
                item.isActive,
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

data class PetCatalogAdminListResponse(
    val pets: List<PetCatalogAdminItemResponse>,
) {
    companion object {
        fun from(items: List<PetCatalogItem>) = PetCatalogAdminListResponse(items.map(PetCatalogAdminItemResponse::from))
    }
}
