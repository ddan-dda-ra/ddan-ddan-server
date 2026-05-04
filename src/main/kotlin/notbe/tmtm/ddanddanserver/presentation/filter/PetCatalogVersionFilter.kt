package notbe.tmtm.ddanddanserver.presentation.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import notbe.tmtm.ddanddanserver.application.service.PetCatalogService
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class PetCatalogVersionFilter(
    private val petCatalogService: PetCatalogService,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        response.setHeader(HEADER_NAME, petCatalogService.currentVersion().toString())
        filterChain.doFilter(request, response)
    }

    companion object {
        const val HEADER_NAME = "X-Pet-Catalog-Version"
    }
}
