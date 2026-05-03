package notbe.tmtm.ddanddanserver.application.event

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType
import notbe.tmtm.ddanddanserver.infrastructure.api.DiscordHookApi
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.time.Instant
import kotlin.test.assertTrue

/**
 * 회귀 방지 통합 테스트.
 *
 * 단위 테스트(UserRegisteredEventListenerTest)는 listener.handle()을 직접 호출해
 * @TransactionalEventListener · @Async 어노테이션 동작을 검증할 수 없다.
 *
 * 본 테스트는 Spring TestContext로 listener를 등록하고 ApplicationEventPublisher를
 * 통해 이벤트를 발행하여, fallbackExecution = true 동작이 유지되는지 검증한다.
 * 트랜잭션 매니저를 등록하지 않아 dev 환경(단일 MongoDB로 @Transactional이 NO-OP인 상황)을 그대로 시뮬레이션한다.
 */
@SpringJUnitConfig(
    classes = [
        UserRegisteredEventListener::class,
        UserRegisteredEventListenerIntegrationTest.TestConfig::class,
    ],
)
@TestPropertySource(properties = ["spring.profiles.active=test"])
class UserRegisteredEventListenerIntegrationTest {
    @TestConfiguration
    class TestConfig {
        @Bean
        fun userRepository(): UserRepository = mockk()

        @Bean
        fun discordHookApi(): DiscordHookApi = mockk(relaxed = true)
    }

    @Autowired
    lateinit var publisher: ApplicationEventPublisher

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var discordHookApi: DiscordHookApi

    @Test
    fun `트랜잭션 매니저가 없는 환경에서도 publishEvent 후 listener가 호출되어 디스코드 웹훅이 발송된다`() {
        every { userRepository.count() } returns 42L
        val captured = slot<DiscordHookApi.Request>()
        every { discordHookApi.sendMessage(capture(captured)) } returns Unit

        publisher.publishEvent(
            UserRegisteredEvent(
                userId = ObjectId(),
                nickName = "통합테스트사용자",
                oAuthType = OAuthType.KAKAO,
                registeredAt = Instant.now(),
            ),
        )

        verify(exactly = 1) { discordHookApi.sendMessage(any()) }
        assertTrue(captured.captured.content.contains("[TEST] 신규 가입"))
        assertTrue(captured.captured.content.contains("provider=KAKAO"))
        assertTrue(captured.captured.content.contains("누적 가입자=42명"))
    }
}
