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
private const val MAX_RETRY_COUNT = 3
private const val RETRY_DELAY_MS = 500L

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
            .map { token -> buildMessage(token.value, message, routingView) }

        executeWithRetry("sendToMultiple") {
            fcmClient.sendEach(requests)
        }
    }

    @Async
    override fun sendToUser(deviceToken: DeviceToken?, message: String, routingView: RoutingView) {
        if (deviceToken?.isValid() != true) return

        val request = buildMessage(deviceToken.value, message, routingView)

        executeWithRetry("sendToUser") {
            fcmClient.send(request)
        }
    }

    private fun buildMessage(token: String, message: String, routingView: RoutingView): Message =
        Message
            .builder()
            .setToken(token)
            .setNotification(Notification.builder().setBody(message).build())
            .putData(ROUTING_VIEW, routingView.name)
            .build()

    private fun executeWithRetry(methodName: String, action: () -> Unit) {
        repeat(MAX_RETRY_COUNT) { attempt ->
            try {
                action()
                return
            } catch (e: Exception) {
                logger().warn("FCM $methodName 전송 실패 (시도 ${attempt + 1}/$MAX_RETRY_COUNT)", e)
                if (attempt < MAX_RETRY_COUNT - 1) {
                    Thread.sleep(RETRY_DELAY_MS)
                }
            }
        }
        logger().error("FCM $methodName 전송 최종 실패: ${MAX_RETRY_COUNT}회 재시도 후에도 실패")
    }
}
