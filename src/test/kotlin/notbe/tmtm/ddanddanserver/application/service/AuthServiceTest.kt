package notbe.tmtm.ddanddanserver.application.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import notbe.tmtm.ddanddanserver.application.event.UserRegisteredEvent
import notbe.tmtm.ddanddanserver.application.processor.OAuth
import notbe.tmtm.ddanddanserver.application.processor.OAuthProcessor
import notbe.tmtm.ddanddanserver.application.processor.OAuthProcessorFactory
import notbe.tmtm.ddanddanserver.common.JWTTokenProvider
import notbe.tmtm.ddanddanserver.domain.model.auth.Auth
import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType
import notbe.tmtm.ddanddanserver.domain.model.user.DeviceToken
import notbe.tmtm.ddanddanserver.domain.model.user.User
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.AuthRepository
import notbe.tmtm.ddanddanserver.infrastructure.database.repository.UserRepository
import org.bson.types.ObjectId
import org.springframework.context.ApplicationEventPublisher
import java.util.Optional

class AuthServiceTest : FunSpec({
    lateinit var authRepository: AuthRepository
    lateinit var userRepository: UserRepository
    lateinit var jwtTokenProvider: JWTTokenProvider
    lateinit var oauthProcessorFactory: OAuthProcessorFactory
    lateinit var eventPublisher: ApplicationEventPublisher
    lateinit var oauthProcessor: OAuthProcessor
    lateinit var authService: AuthService

    beforeEach {
        authRepository = mockk()
        userRepository = mockk()
        jwtTokenProvider = mockk()
        oauthProcessorFactory = mockk()
        eventPublisher = mockk(relaxed = true)
        oauthProcessor = mockk()
        authService =
            AuthService(
                authRepository = authRepository,
                userRepository = userRepository,
                jwtTokenProvider = jwtTokenProvider,
                oauthProcessorFactory = oauthProcessorFactory,
                eventPublisher = eventPublisher,
            )

        every { jwtTokenProvider.createAccessToken(any()) } returns "access-token"
        every { jwtTokenProvider.createRefreshToken(any()) } returns "refresh-token"
    }

    test("신규 사용자 가입 시 UserRegisteredEvent가 발행된다") {
        val oAuthAccessToken = "kakao-access"
        val oAuth = OAuth(id = "oauth-id-1", type = OAuthType.KAKAO, nickName = "신규유저")

        every { oauthProcessorFactory.getClient(OAuthType.KAKAO) } returns oauthProcessor
        every { oauthProcessor.getOAuth(oAuthAccessToken) } returns oAuth
        every { authRepository.findByOAuthIdAndType(oAuth.id, OAuthType.KAKAO) } returns null
        every { userRepository.save(any<User>()) } answers { firstArg() }
        every { authRepository.save(any<Auth>()) } answers { firstArg() }

        val capturedEvent = slot<UserRegisteredEvent>()
        every { eventPublisher.publishEvent(capture(capturedEvent)) } returns Unit

        val result = authService.login(oAuthAccessToken, OAuthType.KAKAO, deviceToken = "device-1")

        result.accessToken shouldBe "access-token"
        result.refreshToken shouldBe "refresh-token"
        result.user.name shouldBe "신규유저"

        verify(exactly = 1) { eventPublisher.publishEvent(any<UserRegisteredEvent>()) }

        capturedEvent.captured.nickName shouldBe "신규유저"
        capturedEvent.captured.oAuthType shouldBe OAuthType.KAKAO
        capturedEvent.captured.userId shouldBe result.user.id
    }

    test("기존 사용자 로그인 시 UserRegisteredEvent가 발행되지 않는다") {
        val oAuthAccessToken = "kakao-access"
        val oAuth = OAuth(id = "oauth-id-2", type = OAuthType.KAKAO, nickName = "기존유저")
        val existingUserId = ObjectId()
        val existingUser =
            User(
                id = existingUserId,
                deviceToken = null,
                name = "기존유저",
            )
        val existingAuth = Auth.create(oAuthId = oAuth.id, type = OAuthType.KAKAO, userId = existingUserId)

        every { oauthProcessorFactory.getClient(OAuthType.KAKAO) } returns oauthProcessor
        every { oauthProcessor.getOAuth(oAuthAccessToken) } returns oAuth
        every { authRepository.findByOAuthIdAndType(oAuth.id, OAuthType.KAKAO) } returns existingAuth
        every { userRepository.findById(existingUserId) } returns Optional.of(existingUser)
        every { userRepository.save(any<User>()) } answers { firstArg() }

        val result = authService.login(oAuthAccessToken, OAuthType.KAKAO, deviceToken = "new-device")

        result.user.id shouldBe existingUserId
        verify(exactly = 0) { eventPublisher.publishEvent(any<UserRegisteredEvent>()) }
    }

    test("기존 사용자가 동일한 deviceToken으로 로그인하면 사용자 저장이 호출되지 않는다") {
        val oAuthAccessToken = "apple-access"
        val oAuth = OAuth(id = "oauth-id-3", type = OAuthType.APPLE, nickName = "동일토큰유저")
        val existingUserId = ObjectId()
        val existingUser =
            User(
                id = existingUserId,
                deviceToken = DeviceToken("same-token"),
                name = "동일토큰유저",
            )
        val existingAuth = Auth.create(oAuthId = oAuth.id, type = OAuthType.APPLE, userId = existingUserId)

        every { oauthProcessorFactory.getClient(OAuthType.APPLE) } returns oauthProcessor
        every { oauthProcessor.getOAuth(oAuthAccessToken) } returns oAuth
        every { authRepository.findByOAuthIdAndType(oAuth.id, OAuthType.APPLE) } returns existingAuth
        every { userRepository.findById(existingUserId) } returns Optional.of(existingUser)

        authService.login(oAuthAccessToken, OAuthType.APPLE, deviceToken = "same-token")

        verify(exactly = 0) { userRepository.save(any<User>()) }
        verify(exactly = 0) { eventPublisher.publishEvent(any<UserRegisteredEvent>()) }
    }
})
