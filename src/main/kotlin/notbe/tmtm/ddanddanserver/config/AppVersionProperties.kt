package notbe.tmtm.ddanddanserver.config

import notbe.tmtm.ddanddanserver.domain.model.version.SemanticVersion
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 플랫폼별 최소 지원 앱 버전 설정.
 * 요청 헤더의 앱 버전이 이 값보다 낮으면 426 Upgrade Required로 응답한다.
 */
@ConfigurationProperties(prefix = "app.version.minimum")
data class AppVersionProperties(
    val android: String = DEFAULT_MINIMUM_VERSION,
    val ios: String = DEFAULT_MINIMUM_VERSION,
) {
    init {
        requireSemanticVersion("app.version.minimum.android", android)
        requireSemanticVersion("app.version.minimum.ios", ios)
    }

    companion object {
        const val DEFAULT_MINIMUM_VERSION = "1.5.0"

        private fun requireSemanticVersion(key: String, value: String) {
            require(runCatching { SemanticVersion.parse(value) }.isSuccess) {
                "$key 설정값이 x.y.z 형식이 아닙니다: $value"
            }
        }
    }
}
