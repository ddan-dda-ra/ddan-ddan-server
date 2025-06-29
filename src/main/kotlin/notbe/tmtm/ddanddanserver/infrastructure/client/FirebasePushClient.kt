package notbe.tmtm.ddanddanserver.infrastructure.client

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import notbe.tmtm.ddanddanserver.domain.model.notification.RoutingView
import org.springframework.stereotype.Component

private const val ROUTING_VIEW = "routingView"

@Component
class FirebasePushClient(
    private val fcmClient: FirebaseMessaging,
) : PushClient {

    override fun sendToMultiple(deviceTokens: List<String>, message: String, routingView: RoutingView) {
        if (deviceTokens.isEmpty()) return

        val requests = deviceTokens
            .filter { isValidDeviceToken(it) }
            .map { token ->
                Message
                    .builder()
                    .setToken(token)
                    .setNotification(Notification.builder().setBody(message).build())
                    .putData(ROUTING_VIEW, routingView.name)
                    .build()
            }

        fcmClient.sendEach(requests)
    }

    override fun sendToUser(deviceToken: String, message: String, routingView: RoutingView) {
        if (isValidDeviceToken(deviceToken).not()) return

        val request = Message
            .builder()
            .setToken(deviceToken)
            .setNotification(Notification.builder().setBody(message).build())
            .putData(ROUTING_VIEW, routingView.name)
            .build()

        fcmClient.send(request)
    }

    private fun isValidDeviceToken(deviceToken: String) = deviceToken.isNotBlank() && deviceToken != "deviceToken"
}
