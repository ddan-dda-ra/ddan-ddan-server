package notbe.tmtm.ddanddanserver.infrastructure.api

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.client.ResourceAccessException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import kotlin.concurrent.thread
import kotlin.test.assertTrue

/**
 * 외부 API RestClient 타임아웃 통합 테스트.
 *
 * 타임아웃은 `@Value`로 주입한 Duration 설정이 RestClient까지 전달돼야 동작하므로, 실제 스프링 컨텍스트로 확인한다.
 * 연결은 받지만 응답하지 않는 로컬 서버에 요청해서 read 타임아웃 안에 실패하는지 본다.
 */
@SpringBootTest(
    classes = [ApiConfiguration::class],
    properties = ["external-api.timeout.read=300ms"],
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiConfigurationTimeoutIntegrationTest {

    @Autowired
    lateinit var slackHookApi: SlackHookApi

    @Test
    @Timeout(10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    fun `응답하지 않는 외부 API는 read 타임아웃으로 실패해야 한다`() {
        val startedAt = System.nanoTime()

        val exception = assertThrows<ResourceAccessException> {
            slackHookApi.sendMessage(SlackHookApi.Request(text = "timeout-test"))
        }

        val elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000
        assertTrue(exception.cause is SocketTimeoutException, "cause: ${exception.cause}")
        assertTrue(elapsedMillis < 3_000, "타임아웃까지 ${elapsedMillis}ms 걸림")
    }

    @AfterAll
    fun tearDown() {
        silentServer.close()
        acceptedSockets.forEach { it.close() }
    }

    companion object {
        private val silentServer = ServerSocket(0)
        private val acceptedSockets = mutableListOf<Socket>()

        init {
            thread(isDaemon = true) {
                runCatching {
                    while (true) {
                        acceptedSockets += silentServer.accept()
                    }
                }
            }
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            val url = "http://127.0.0.1:${silentServer.localPort}"
            registry.add("slack.hook-url") { url }
            registry.add("discord.hook-url") { url }
        }
    }
}
