package notbe.tmtm.ddanddanserver.presentation.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import notbe.tmtm.ddanddanserver.application.service.PetCatalogService
import notbe.tmtm.ddanddanserver.common.util.logger
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Instant

class PetCatalogRevisionFilter(
    private val petCatalogService: PetCatalogService,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val downloadRequired =
            runCatching {
                val clientRevision = request.getHeader(REQUEST_REVISION_HEADER)?.toInstantOrNull()
                clientRevision == null || !clientRevision.equals(petCatalogService.currentRevision())
            }.getOrElse {
                logger().warn("펫 카탈로그 revision 비교 실패, 갱신 필요로 응답합니다", it)
                true
            }
        response.setHeader(DOWNLOAD_REQUIRED_HEADER, downloadRequired.toString())
        filterChain.doFilter(request, response)
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        !request.requestURI.startsWith(API_PREFIX) || request.requestURI == LOGIN_PATH

    private fun String.toInstantOrNull(): Instant? = runCatching(Instant::parse).getOrNull()

    companion object {
        const val REQUEST_REVISION_HEADER = "X-Pet-Catalog-Version"
        const val DOWNLOAD_REQUIRED_HEADER = "X-Pet-Catalog-Download-Required"

        private const val API_PREFIX = "/v1/"
        private const val LOGIN_PATH = "/v1/auth/login"
    }
}
