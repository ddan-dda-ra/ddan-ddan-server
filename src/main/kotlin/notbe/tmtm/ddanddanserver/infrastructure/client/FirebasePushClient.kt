package notbe.tmtm.ddanddanserver.infrastructure.client

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import notbe.tmtm.ddanddanserver.common.util.logger
import notbe.tmtm.ddanddanserver.domain.model.notification.RoutingView
import notbe.tmtm.ddanddanserver.domain.model.user.DeviceToken
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

private const val ROUTING_VIEW = "routingView"

@Component
class FirebasePushClient(
    private val fcmClient: FirebaseMessaging,
) : PushClient {

    @Async
    override fun sendToMultiple(deviceTokens: List<DeviceToken?>, message: String, routingView: RoutingView) {
        if (deviceTokens.isEmpty()) return

        val requests = deviceTokens
            .filterNotNull()
            .filter { it.isValid() }
            .map { token ->
                Message
                    .builder()
                    .setToken(token.value)
                    .setNotification(Notification.builder().setBody(message).build())
                    .putData(ROUTING_VIEW, routingView.name)
                    .build()
            }

        fcmClient.sendEach(requests)
    }

    @Async
    override fun sendToUser(deviceToken: DeviceToken?, message: String, routingView: RoutingView) {
        if (deviceToken?.isValid() != true) return

        try {
            val request = Message
                .builder()
                .setToken(deviceToken.value)
                .setNotification(Notification.builder().setBody(message).build())
                .putData(ROUTING_VIEW, routingView.name)
                .build()

            fcmClient.send(request)
        } catch (e: Exception) {
            logger().error("FCM 푸시 전송 실패", e)
        }
    }
}
