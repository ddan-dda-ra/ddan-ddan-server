package notbe.tmtm.ddanddanserver.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "fcm")
data class FCMProperties(
    val projectId: String,
    val key: String,
)
