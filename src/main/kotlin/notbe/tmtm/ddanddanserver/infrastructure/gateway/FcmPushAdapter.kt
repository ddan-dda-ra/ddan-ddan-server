package notbe.tmtm.ddanddanserver.infrastructure.gateway

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import notbe.tmtm.ddanddanserver.domain.model.notification.RoutingView
import org.springframework.stereotype.Component

@Component
class FcmPushAdapter(
    private val fcmClient: FirebaseMessaging,
) {
    fun send(
        deviceToken: String,
        content: String,
        routingView: RoutingView,
    ) {
        if (deviceToken == "deviceToken") return
        val request =
            Message
                .builder()
                .setToken(deviceToken)
                .setNotification(Notification.builder().setBody(content).build())
                .putData("routingView", routingView.name)
                .build()

        fcmClient.send(request)
    }

    fun sendAll(
        deviceTokens: List<String>,
        content: String,
        routingView: RoutingView,
    ) {
        val requests =
            deviceTokens
                .filter { (it != "deviceToken") }
                .map { token ->
                    Message
                        .builder()
                        .setToken(token)
                        .setNotification(Notification.builder().setBody(content).build())
                        .putData("routingView", routingView.name)
                        .build()
                }

        fcmClient.sendEach(requests)
    }
}
