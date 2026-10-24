package notbe.tmtm.ddanddanserver.infrastructure.client

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import io.kotest.core.spec.style.FunSpec
import io.mockk.mockk
import io.mockk.verify
import notbe.tmtm.ddanddanserver.domain.model.notification.RoutingView
import notbe.tmtm.ddanddanserver.domain.model.user.DeviceToken
import org.springframework.retry.support.RetryTemplate

class FirebasePushClientTest : FunSpec({
    lateinit var fcmClient: FirebaseMessaging
    lateinit var firebasePushClient: FirebasePushClient

    beforeEach {
        fcmClient = mockk(relaxed = true)
        firebasePushClient = FirebasePushClient(fcmClient, RetryTemplate())
    }

    test("필터링 후 유효한 토큰이 없으면 sendEach를 호출하지 않는다") {
        firebasePushClient.sendToMultiple(
            listOf(null, DeviceToken("deviceToken")),
            "메시지",
            RoutingView.MAIN,
        )

        verify(exactly = 0) { fcmClient.sendEach(any<List<Message>>()) }
    }

    test("유효한 토큰만 골라 sendEach로 전송한다") {
        firebasePushClient.sendToMultiple(
            listOf(null, DeviceToken("deviceToken"), DeviceToken("valid-token")),
            "메시지",
            RoutingView.MAIN,
        )

        verify(exactly = 1) { fcmClient.sendEach(match<List<Message>> { it.size == 1 }) }
    }
})
