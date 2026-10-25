package notbe.tmtm.ddanddanserver.infrastructure.client

import com.google.firebase.messaging.BatchResponse
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import notbe.tmtm.ddanddanserver.common.util.logger
import notbe.tmtm.ddanddanserver.domain.model.notification.RoutingView
import notbe.tmtm.ddanddanserver.domain.model.user.DeviceToken
import org.springframework.retry.support.RetryTemplate
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

private const val ROUTING_VIEW = "routingView"
private const val SEND_EACH_LIMIT = 500

@Component
class FirebasePushClient(
    private val fcmClient: FirebaseMessaging,
    private val retryTemplate: RetryTemplate,
) : PushClient {
    private val log = logger()

    @Async
    override fun sendToMultiple(deviceTokens: List<DeviceToken?>, message: String, routingView: RoutingView) {
        val requests = deviceTokens
            .filterNotNull()
            .filter { it.isValid() }
            .map { token -> buildMessage(token.value, message, routingView) }
        // FCM sendEach는 빈 리스트를 받으면 예외를 던지므로 필터링 후 대상이 없으면 전송하지 않는다.
        if (requests.isEmpty()) return

        // sendEach는 한 번에 최대 500건만 받는다. 빈 리스트도 예외이므로 대상이 없으면 묶음이 생기지 않아 전송하지 않는다.
        requests.chunked(SEND_EACH_LIMIT).forEachIndexed { index, batch ->
            // 한 묶음이 실패해도 나머지 묶음은 전송한다.
            runCatching {
                retryTemplate.execute<BatchResponse, Exception> { fcmClient.sendEach(batch) }
            }.onSuccess {
                log.info("FCM 일괄 전송 ${index + 1}번째 묶음 ${batch.size}건: 성공 ${it.successCount}, 실패 ${it.failureCount}")
            }.onFailure {
                log.error("FCM 일괄 전송 ${index + 1}번째 묶음 ${batch.size}건 실패", it)
            }
        }
    }

    @Async
    override fun sendToUser(deviceToken: DeviceToken?, message: String, routingView: RoutingView) {
        if (deviceToken?.isValid() != true) return

        val request = buildMessage(deviceToken.value, message, routingView)

        retryTemplate.execute<Unit, Exception> {
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
}
