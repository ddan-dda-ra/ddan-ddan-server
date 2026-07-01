package notbe.tmtm.ddanddanserver.domain.model.petcatalog

import notbe.tmtm.ddanddanserver.domain.exception.PetCatalogInvalidException
import java.time.Instant
import java.util.Locale

data class PetCatalog(
    val revision: Instant,
    val pets: List<PetCatalogItem>,
)

class PetCatalogItem private constructor(
    val type: String,
    val name: String,
    val colorCode: String,
    val isActive: Boolean,
    val displayOrder: Int,
    val levels: List<PetCatalogLevel>,
) {
    companion object {
        private val TYPE_REGEX = Regex("^[A-Z][A-Z0-9_]{0,29}$")
        private val COLOR_REGEX = Regex("^#[0-9A-Fa-f]{6}$")
        private val REQUIRED_LEVELS = (1..5).toSet()

        fun create(
            type: String,
            name: String,
            colorCode: String,
            isActive: Boolean,
            displayOrder: Int,
            levels: List<PetCatalogLevel>,
            assetUrlValidator: (PetCatalogAssetUrl) -> Unit = {},
        ): PetCatalogItem {
            val canonicalType = canonicalizeType(type)
            val canonicalName = name.trim()
            val canonicalColorCode = colorCode.trim().uppercase(Locale.ROOT)
            invalidUnless(TYPE_REGEX.matches(canonicalType), "type 형식이 올바르지 않습니다")
            invalidUnless(canonicalName.length in 1..30, "name은 1자 이상 30자 이하여야 합니다")
            invalidUnless(COLOR_REGEX.matches(canonicalColorCode), "colorCode 형식이 올바르지 않습니다")
            invalidUnless(displayOrder >= 0, "displayOrder는 0 이상이어야 합니다")
            invalidUnless(levels.size == REQUIRED_LEVELS.size, "levels는 정확히 5개여야 합니다")
            invalidUnless(levels.map { it.level }.toSet() == REQUIRED_LEVELS, "levels는 1부터 5까지 중복 없이 포함해야 합니다")
            levels.forEach { level ->
                assetUrlValidator(PetCatalogAssetUrl.image(level.imageUrl))
                assetUrlValidator(PetCatalogAssetUrl.lottie(level.lottieDefaultUrl))
                assetUrlValidator(PetCatalogAssetUrl.lottie(level.lottiePlayEatUrl))
            }
            return PetCatalogItem(
                type = canonicalType,
                name = canonicalName,
                colorCode = canonicalColorCode,
                isActive = isActive,
                displayOrder = displayOrder,
                levels = levels.sortedBy { it.level },
            )
        }

        fun canonicalizeType(type: String): String = type.trim().uppercase(Locale.ROOT)

        private fun invalidUnless(
            condition: Boolean,
            message: String,
        ) {
            if (!condition) throw PetCatalogInvalidException(message)
        }
    }
}

data class PetCatalogLevel(
    val level: Int,
    val imageUrl: String,
    val lottieDefaultUrl: String,
    val lottiePlayEatUrl: String,
)
