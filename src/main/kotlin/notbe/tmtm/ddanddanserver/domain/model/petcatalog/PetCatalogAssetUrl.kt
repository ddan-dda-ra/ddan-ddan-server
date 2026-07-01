package notbe.tmtm.ddanddanserver.domain.model.petcatalog

import notbe.tmtm.ddanddanserver.domain.exception.PetCatalogInvalidException
import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

class PetCatalogAssetUrl private constructor(
    val uri: URI,
) {
    companion object {
        private val IMAGE_EXTENSIONS = setOf("svg", "png", "webp")

        fun image(value: String): PetCatalogAssetUrl = create(value, IMAGE_EXTENSIONS)

        fun lottie(value: String): PetCatalogAssetUrl = create(value, setOf("json"))

        private fun create(
            value: String,
            allowedExtensions: Set<String>,
        ): PetCatalogAssetUrl {
            val uri =
                try {
                    URI(value)
                } catch (exception: URISyntaxException) {
                    throw PetCatalogInvalidException("에셋 URL 형식이 올바르지 않습니다")
                }
            val extension = uri.path?.substringAfterLast('.', "")?.lowercase(Locale.ROOT)
            if (
                value.any(Char::isWhitespace) || !uri.isAbsolute || uri.host == null ||
                uri.userInfo != null || uri.fragment != null || extension !in allowedExtensions
            ) {
                throw PetCatalogInvalidException("에셋 URL 형식 또는 확장자가 올바르지 않습니다")
            }
            return PetCatalogAssetUrl(uri)
        }
    }
}
