package notbe.tmtm.ddanddanserver.infrastructure.gateway

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import notbe.tmtm.ddanddanserver.domain.gateway.PushGateway
import org.springframework.stereotype.Component

@Component
class FcmPushGatewayImpl(
    private val fcmClient: FirebaseMessaging,
) : PushGateway {
    override fun send(
        deviceToken: String,
        content: String,
    ) {
        if (deviceToken == "deviceToken") return
        val request =
            Message
                .builder()
                .setToken(deviceToken)
                .setNotification(Notification.builder().setBody(content).build())
                .build()

        fcmClient.send(request)
    }

    override fun sendAll(
        deviceTokens: List<String>,
        content: String,
    ) {
        val requests =
            deviceTokens
                .filter { (it != "deviceToken") }
                .map { token ->
                    Message
                        .builder()
                        .setToken(token)
                        .setNotification(Notification.builder().setBody(content).build())
                        .build()
                }

        fcmClient.sendEach(requests)
    }
}
