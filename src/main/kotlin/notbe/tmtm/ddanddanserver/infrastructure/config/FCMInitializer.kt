package notbe.tmtm.ddanddanserver.infrastructure.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import java.io.IOException
import java.util.Base64

@Component
class FCMInitializer(
    private val fcmProperties: FCMProperties,
) {
    @PostConstruct
    fun init() {
        try {
            val serviceAccount = Base64.getDecoder().decode(fcmProperties.key).inputStream()

            val options: FirebaseOptions =
                FirebaseOptions
                    .builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build()

            FirebaseApp.initializeApp(options)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}
