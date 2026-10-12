package notbe.tmtm.ddanddanserver.presentation.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import notbe.tmtm.ddanddanserver.common.JWTTokenProvider
import notbe.tmtm.ddanddanserver.common.util.logger
import notbe.tmtm.ddanddanserver.domain.exception.AppVersionUpgradeRequiredException
import notbe.tmtm.ddanddanserver.domain.model.version.SemanticVersion
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerExceptionResolver

open class AppVersionFilter(
    private val handlerExceptionResolver: HandlerExceptionResolver,
    private val jwtTokenProvider: JWTTokenProvider,
    androidMinimumVersion: String,
    iosMinimumVersion: String,
) : OncePerRequestFilter() {

    private val log = logger()
    private val androidMinimum = SemanticVersion.parse(androidMinimumVersion)
    private val iosMinimum = SemanticVersion.parse(iosMinimumVersion)

    companion object {
        private const val ANDROID_VERSION_HEADER = "X-Android-Version"
        private const val IOS_VERSION_HEADER = "X-iOS-Version"
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val USER_AGENT_HEADER = "User-Agent"
        private const val BEARER_PREFIX = "Bearer "
        private const val UNKNOWN = "unknown"

        private val EXCLUDED_PATHS = setOf(
            "/actuator",
            "/swagger-ui",
            "/v3/api-docs",
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

            // 헤더가 없으면 통과 (하위 호환성: 헤더를 보내지 않는 클라이언트 보호)
            if (androidVersion.isNullOrBlank() && iosVersion.isNullOrBlank()) {
                filterChain.doFilter(request, response)
                return
            }

            when {
                !androidVersion.isNullOrBlank() -> validateAppVersion(request, androidVersion, "android", androidMinimum)
                !iosVersion.isNullOrBlank() -> validateAppVersion(request, iosVersion, "ios", iosMinimum)
            }

            filterChain.doFilter(request, response)
        } catch (e: Exception) {
            handlerExceptionResolver.resolveException(request, response, null, e)
        }
    }

    private fun shouldSkipValidation(request: HttpServletRequest): Boolean {
        val requestURI = request.requestURI
        return EXCLUDED_PATHS.any { requestURI.startsWith(it) }
    }

    private fun validateAppVersion(
        request: HttpServletRequest,
        version: String,
        platform: String,
        minimum: SemanticVersion,
    ) {
        val current = runCatching { SemanticVersion.parse(version) }.getOrElse {
            logBlocked(request, "앱 버전 형식 오류로 차단", platform, version, minimum)
            throw AppVersionUpgradeRequiredException(
                platform = platform,
                currentVersion = version,
                minimumVersion = minimum.toString(),
            )
        }

        if (current < minimum) {
            logBlocked(request, "최소 버전 미만으로 차단", platform, version, minimum)
            throw AppVersionUpgradeRequiredException(
                platform = platform,
                currentVersion = version,
                minimumVersion = minimum.toString(),
            )
        }
    }

    private fun logBlocked(
        request: HttpServletRequest,
        reason: String,
        platform: String,
        version: String,
        minimum: SemanticVersion,
    ) {
        log.info(
            "{}: platform={}, version={}, minimum={}, userId={}, method={}, uri={}, userAgent={}",
            reason,
            platform,
            version,
            minimum,
            extractUserId(request),
            request.method,
            request.requestURI,
            request.getHeader(USER_AGENT_HEADER) ?: UNKNOWN,
        )
    }

    // 버전 검사는 인증 전에 수행되므로, 로깅 목적에 한해 토큰에서 유저 ID를 best-effort로 추출한다.
    private fun extractUserId(request: HttpServletRequest): String =
        request.getHeader(AUTHORIZATION_HEADER)
            ?.takeIf { it.startsWith(BEARER_PREFIX, ignoreCase = true) }
            ?.substring(BEARER_PREFIX.length)
            ?.trim()
            ?.let { runCatching { jwtTokenProvider.extractUserIdOrNull(it) }.getOrNull() }
            ?.toHexString()
            ?: UNKNOWN
}
