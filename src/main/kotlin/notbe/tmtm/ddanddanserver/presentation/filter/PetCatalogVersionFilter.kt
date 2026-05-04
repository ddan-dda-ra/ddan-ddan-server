package notbe.tmtm.ddanddanserver.presentation.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import notbe.tmtm.ddanddanserver.application.service.PetCatalogService
import notbe.tmtm.ddanddanserver.common.util.logger
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
        // 헤더는 부가 정보이므로 catalog 조회 실패가 본 요청 처리를 차단하지 않도록 흡수
        try {
            response.setHeader(HEADER_NAME, petCatalogService.currentVersion().toString())
        } catch (e: Exception) {
            logger().warn("X-Pet-Catalog-Version 헤더 설정 실패, 요청 처리는 계속 진행", e)
        }
        filterChain.doFilter(request, response)
    }

    companion object {
        const val HEADER_NAME = "X-Pet-Catalog-Version"
    }
}
