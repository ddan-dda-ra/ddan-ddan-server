package notbe.tmtm.ddanddanserver.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.IOException
import java.util.Base64

@Configuration
class FcmConfig {
    @Bean
    fun fcmClient(firebaseApp: FirebaseApp): FirebaseMessaging = FirebaseMessaging.getInstance(firebaseApp)

    @Bean
    fun firebaseApp(fcmProperties: FCMProperties): FirebaseApp {
        try {
            val serviceAccount = Base64.getDecoder().decode(fcmProperties.key).inputStream()

            val options: FirebaseOptions =
                FirebaseOptions
                    .builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build()

            return FirebaseApp.initializeApp(options)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}

@ConfigurationProperties(prefix = "fcm")
data class FCMProperties(
    val projectId: String,
    val key: String,
)
