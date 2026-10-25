package notbe.tmtm.ddanddanserver.infrastructure.client

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import notbe.tmtm.ddanddanserver.domain.model.notification.RoutingView
import notbe.tmtm.ddanddanserver.domain.model.user.DeviceToken
import org.springframework.retry.support.RetryTemplate

class FirebasePushClientBatchTest : FunSpec({
    lateinit var fcmClient: FirebaseMessaging
    lateinit var firebasePushClient: FirebasePushClient

    beforeEach {
        fcmClient = mockk(relaxed = true)
        firebasePushClient = FirebasePushClient(fcmClient, RetryTemplate.builder().maxAttempts(1).build())
    }

    fun tokens(count: Int) = (1..count).map { DeviceToken("token-$it") }

    test("500건을 넘으면 500건씩 나눠서 전송한다") {
        val batchSizes = mutableListOf<Int>()
        every { fcmClient.sendEach(any<List<Message>>()) } answers {
            batchSizes += firstArg<List<Message>>().size
            mockk(relaxed = true)
        }

        firebasePushClient.sendToMultiple(tokens(1201), "메시지", RoutingView.MAIN)

        batchSizes shouldBe listOf(500, 500, 201)
    }

    test("500건 이하는 한 번에 전송한다") {
        firebasePushClient.sendToMultiple(tokens(500), "메시지", RoutingView.MAIN)

        verify(exactly = 1) { fcmClient.sendEach(match<List<Message>> { it.size == 500 }) }
    }

    test("한 묶음이 실패해도 나머지 묶음은 전송한다") {
        var call = 0
        every { fcmClient.sendEach(any<List<Message>>()) } answers {
            call++
            if (call == 1) throw IllegalStateException("fcm down")
            mockk(relaxed = true)
        }

        firebasePushClient.sendToMultiple(tokens(1000), "메시지", RoutingView.MAIN)

        verify(exactly = 2) { fcmClient.sendEach(any<List<Message>>()) }
    }

    test("유효한 토큰이 없으면 전송하지 않는다") {
        firebasePushClient.sendToMultiple(listOf(null, DeviceToken("deviceToken")), "메시지", RoutingView.MAIN)
        firebasePushClient.sendToMultiple(emptyList(), "메시지", RoutingView.MAIN)

        verify(exactly = 0) { fcmClient.sendEach(any<List<Message>>()) }
    }
})
