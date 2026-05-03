package notbe.tmtm.ddanddanserver.application.event

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType
import notbe.tmtm.ddanddanserver.infrastructure.api.DiscordHookApi
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import org.bson.types.ObjectId
import java.time.Instant

class UserRegisteredEventListenerTest : FunSpec({
    lateinit var userRepository: UserRepository
    lateinit var discordHookApi: DiscordHookApi
    lateinit var listener: UserRegisteredEventListener

    fun newListener(activeProfile: String): UserRegisteredEventListener =
        UserRegisteredEventListener(
            userRepository = userRepository,
            discordHookApi = discordHookApi,
            activeProfile = activeProfile,
        )

    beforeEach {
        userRepository = mockk()
        discordHookApi = mockk(relaxed = true)
        listener = newListener(activeProfile = "prod")
    }

    test("신규 가입 이벤트 수신 시 디스코드 웹훅에 embed로 발송한다") {
        val userId = ObjectId()
        val registeredAt = Instant.parse("2026-05-02T10:15:30Z")
        val event =
            UserRegisteredEvent(
                userId = userId,
                nickName = "홍길동",
                oAuthType = OAuthType.KAKAO,
                registeredAt = registeredAt,
            )
        every { userRepository.count() } returns 1234L
        val captured = slot<DiscordHookApi.Request>()
        every { discordHookApi.sendMessage(capture(captured)) } returns Unit

        listener.handle(event)

        verify(exactly = 1) { userRepository.count() }
        verify(exactly = 1) { discordHookApi.sendMessage(any()) }

        captured.captured.content shouldBe null
        val embed = captured.captured.embeds!!.first()
        embed.title shouldBe "🍫 신규 가입"
        embed.color shouldBe 0xFEE500
        embed.timestamp shouldBe registeredAt.toString()
        embed.footer!!.text shouldBe "🚀 PROD · ddan-ddan-server"
        embed.fields!!.find { it.name == "닉네임" }!!.value shouldBe "홍길동"
        embed.fields.find { it.name == "Provider" }!!.value shouldBe "KAKAO"
        embed.fields.find { it.name == "User ID" }!!.value shouldBe "`$userId`"
        embed.fields.find { it.name == "가입 시각" }!!.value shouldBe "2026-05-02 19:15:30 KST"
        embed.fields.find { it.name == "누적 가입자" }!!.value shouldBe "1,234명"
    }

    test("APPLE provider는 다른 이모지와 색상을 사용한다") {
        listener = newListener(activeProfile = "dev")

        val event =
            UserRegisteredEvent(
                userId = ObjectId(),
                nickName = "tester@example.com",
                oAuthType = OAuthType.APPLE,
                registeredAt = Instant.parse("2026-01-01T00:00:00Z"),
            )
        every { userRepository.count() } returns 7L
        val captured = slot<DiscordHookApi.Request>()
        every { discordHookApi.sendMessage(capture(captured)) } returns Unit

        listener.handle(event)

        val embed = captured.captured.embeds!!.first()
        embed.title shouldBe "🍎 신규 가입"
        embed.color shouldBe 0x1C1C1E
        embed.footer!!.text shouldBe "🧪 DEV · ddan-ddan-server"
        embed.fields!!.find { it.name == "Provider" }!!.value shouldBe "APPLE"
        embed.fields.find { it.name == "누적 가입자" }!!.value shouldBe "7명"
    }

    test("누적 가입자가 100 단위 마일스톤이면 description에 표시된다") {
        val event =
            UserRegisteredEvent(
                userId = ObjectId(),
                nickName = "마일스톤",
                oAuthType = OAuthType.KAKAO,
                registeredAt = Instant.now(),
            )
        every { userRepository.count() } returns 100L
        val captured = slot<DiscordHookApi.Request>()
        every { discordHookApi.sendMessage(capture(captured)) } returns Unit

        listener.handle(event)

        captured.captured.embeds!!.first().description!! shouldContain "🎯"
        captured.captured.embeds!!.first().description!! shouldContain "100명 달성"
    }

    test("누적 가입자가 1000 단위 마일스톤이면 description에 표시된다") {
        val event =
            UserRegisteredEvent(
                userId = ObjectId(),
                nickName = "큰마일스톤",
                oAuthType = OAuthType.KAKAO,
                registeredAt = Instant.now(),
            )
        every { userRepository.count() } returns 1000L
        val captured = slot<DiscordHookApi.Request>()
        every { discordHookApi.sendMessage(capture(captured)) } returns Unit

        listener.handle(event)

        captured.captured.embeds!!.first().description!! shouldContain "🎉"
        captured.captured.embeds!!.first().description!! shouldContain "1,000명 달성"
    }

    test("일반 카운트는 description이 null이다") {
        val event =
            UserRegisteredEvent(
                userId = ObjectId(),
                nickName = "일반",
                oAuthType = OAuthType.KAKAO,
                registeredAt = Instant.now(),
            )
        every { userRepository.count() } returns 17L
        val captured = slot<DiscordHookApi.Request>()
        every { discordHookApi.sendMessage(capture(captured)) } returns Unit

        listener.handle(event)

        captured.captured.embeds!!.first().description shouldBe null
    }

    test("누적 가입자 수가 천 단위 이상이면 콤마가 포함된다") {
        val event =
            UserRegisteredEvent(
                userId = ObjectId(),
                nickName = "집계테스트",
                oAuthType = OAuthType.KAKAO,
                registeredAt = Instant.now(),
            )
        every { userRepository.count() } returns 1_234_567L
        val captured = slot<DiscordHookApi.Request>()
        every { discordHookApi.sendMessage(capture(captured)) } returns Unit

        listener.handle(event)

        captured.captured.embeds!!.first().fields!!
            .find { it.name == "누적 가입자" }!!
            .value shouldBe "1,234,567명"
    }

    test("디스코드 웹훅 호출이 예외를 던져도 리스너는 예외를 흡수한다") {
        val event =
            UserRegisteredEvent(
                userId = ObjectId(),
                nickName = "예외사용자",
                oAuthType = OAuthType.KAKAO,
                registeredAt = Instant.now(),
            )
        every { userRepository.count() } returns 10L
        every { discordHookApi.sendMessage(any()) } throws RuntimeException("webhook down")

        shouldNotThrow<Throwable> {
            listener.handle(event)
        }

        verify(exactly = 1) { discordHookApi.sendMessage(any()) }
    }

    test("userRepository.count() 호출이 예외를 던져도 리스너는 예외를 흡수한다") {
        val event =
            UserRegisteredEvent(
                userId = ObjectId(),
                nickName = "DB장애",
                oAuthType = OAuthType.APPLE,
                registeredAt = Instant.now(),
            )
        every { userRepository.count() } throws RuntimeException("mongo down")

        shouldNotThrow<Throwable> {
            listener.handle(event)
        }

        verify(exactly = 0) { discordHookApi.sendMessage(any()) }
    }

    test("디스코드 요청의 username 필드는 기본값을 따른다") {
        val event =
            UserRegisteredEvent(
                userId = ObjectId(),
                nickName = "기본유저네임",
                oAuthType = OAuthType.KAKAO,
                registeredAt = Instant.now(),
            )
        every { userRepository.count() } returns 1L
        val captured = slot<DiscordHookApi.Request>()
        every { discordHookApi.sendMessage(capture(captured)) } returns Unit

        listener.handle(event)

        captured.captured.username shouldBe DiscordHookApi.DEFAULT_USERNAME
    }
})
