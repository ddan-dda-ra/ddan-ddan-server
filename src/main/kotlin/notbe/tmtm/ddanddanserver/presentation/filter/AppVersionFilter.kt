package notbe.tmtm.ddanddanserver.presentation.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import notbe.tmtm.ddanddanserver.domain.exception.AppVersionUpgradeRequiredException
import notbe.tmtm.ddanddanserver.domain.model.version.SemanticVersion
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerExceptionResolver

open class AppVersionFilter(
    private val handlerExceptionResolver: HandlerExceptionResolver,
) : OncePerRequestFilter() {
    
    companion object {
        private const val ANDROID_VERSION_HEADER = "X-Android-Version"
        private const val IOS_VERSION_HEADER = "X-iOS-Version"
        
        private val EXCLUDED_PATHS = setOf(
            "/actuator",
            "/swagger-ui",
            "/v3/api-docs"
        )
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            if (shouldSkipValidation(request)) {
                filterChain.doFilter(request, response)
                return
            }

            val androidVersion = request.getHeader(ANDROID_VERSION_HEADER)
            val iosVersion = request.getHeader(IOS_VERSION_HEADER)

            // 헤더가 없으면 무조건 통과 (하위 호환성)
            if (androidVersion.isNullOrBlank() && iosVersion.isNullOrBlank()) {
                filterChain.doFilter(request, response)
                return
            }

            // 버전 검증 수행 (1차에서는 기본 구조만 구현)
            validateAppVersion(androidVersion, iosVersion)
            
            filterChain.doFilter(request, response)
        } catch (e: Exception) {
            handlerExceptionResolver.resolveException(request, response, null, e)
        }
    }

    private fun shouldSkipValidation(request: HttpServletRequest): Boolean {
        val requestURI = request.requestURI
        return EXCLUDED_PATHS.any { requestURI.startsWith(it) }
    }

    private fun validateAppVersion(androidVersion: String?, iosVersion: String?) {
        // 1차 구현에서는 헤더 파싱과 기본 구조만 구현
        // 향후 2차에서 실제 버전 비교 로직 추가
        
        when {
            !androidVersion.isNullOrBlank() -> {
                // Android 버전 검증 (향후 구현)
                // 현재는 버전 형식만 검증
                validateVersionFormat(androidVersion, "android")
            }
            !iosVersion.isNullOrBlank() -> {
                // iOS 버전 검증 (향후 구현) 
                // 현재는 버전 형식만 검증
                validateVersionFormat(iosVersion, "ios")
            }
        }
    }

    private fun validateVersionFormat(version: String, platform: String) {
        try {
            SemanticVersion.parse(version)
            // 향후 2차 구현에서 실제 최소 버전과 비교하는 로직 추가
            // 현재는 426 에러를 발생시키지 않고 통과
        } catch (e: IllegalArgumentException) {
            throw AppVersionUpgradeRequiredException(
                platform = platform,
                currentVersion = version,
                minimumVersion = "형식 오류"
            )
        }
    }
}
