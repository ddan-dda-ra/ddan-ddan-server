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
import org.springframework.dao.DuplicateKeyException
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

        every { oauthProcessorFactory.getClient(OAuthType.KAKAO, false) } returns oauthProcessor
        every { oauthProcessor.getOAuth(oAuthAccessToken) } returns oAuth
        every { authRepository.findByOAuthIdAndType(oAuth.id, OAuthType.KAKAO) } returns null
        every { userRepository.save(any<User>()) } answers { firstArg() }
        every { authRepository.save(any<Auth>()) } answers { firstArg() }

        val capturedEvent = slot<UserRegisteredEvent>()
        every { eventPublisher.publishEvent(capture(capturedEvent)) } returns Unit

        val result = authService.login(oAuthAccessToken, OAuthType.KAKAO, deviceToken = "device-1", useMock = false)

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

        every { oauthProcessorFactory.getClient(OAuthType.KAKAO, false) } returns oauthProcessor
        every { oauthProcessor.getOAuth(oAuthAccessToken) } returns oAuth
        every { authRepository.findByOAuthIdAndType(oAuth.id, OAuthType.KAKAO) } returns existingAuth
        every { userRepository.findById(existingUserId) } returns Optional.of(existingUser)
        every { userRepository.updateDeviceToken(existingUserId, DeviceToken("new-device")) } returns Unit

        val result = authService.login(oAuthAccessToken, OAuthType.KAKAO, deviceToken = "new-device", useMock = false)

        result.user.id shouldBe existingUserId
        verify(exactly = 1) { userRepository.updateDeviceToken(existingUserId, DeviceToken("new-device")) }
        verify(exactly = 0) { userRepository.save(any<User>()) }
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

        every { oauthProcessorFactory.getClient(OAuthType.APPLE, false) } returns oauthProcessor
        every { oauthProcessor.getOAuth(oAuthAccessToken) } returns oAuth
        every { authRepository.findByOAuthIdAndType(oAuth.id, OAuthType.APPLE) } returns existingAuth
        every { userRepository.findById(existingUserId) } returns Optional.of(existingUser)

        authService.login(oAuthAccessToken, OAuthType.APPLE, deviceToken = "same-token", useMock = false)

        verify(exactly = 0) { userRepository.save(any<User>()) }
        verify(exactly = 0) { userRepository.updateDeviceToken(any(), any()) }
        verify(exactly = 0) { eventPublisher.publishEvent(any<UserRegisteredEvent>()) }
    }

    test("useMock=true가 전달되면 factory에 useMock=true로 전파된다") {
        val oAuthAccessToken = "mock-kakao-token"
        val oAuth = OAuth(id = "mock-oauth-id", type = OAuthType.KAKAO, nickName = "목유저")

        every { oauthProcessorFactory.getClient(OAuthType.KAKAO, true) } returns oauthProcessor
        every { oauthProcessor.getOAuth(oAuthAccessToken) } returns oAuth
        every { authRepository.findByOAuthIdAndType(oAuth.id, OAuthType.KAKAO) } returns null
        every { userRepository.save(any<User>()) } answers { firstArg() }
        every { authRepository.save(any<Auth>()) } answers { firstArg() }

        authService.login(oAuthAccessToken, OAuthType.KAKAO, deviceToken = null, useMock = true)

        verify(exactly = 1) { oauthProcessorFactory.getClient(OAuthType.KAKAO, true) }
        verify(exactly = 0) { oauthProcessorFactory.getClient(OAuthType.KAKAO, false) }
    }

    test("useMock=false가 전달되면 factory에 useMock=false로 전파된다") {
        val oAuthAccessToken = "real-kakao-token"
        val oAuth = OAuth(id = "real-oauth-id", type = OAuthType.KAKAO, nickName = "실유저")

        every { oauthProcessorFactory.getClient(OAuthType.KAKAO, false) } returns oauthProcessor
        every { oauthProcessor.getOAuth(oAuthAccessToken) } returns oAuth
        every { authRepository.findByOAuthIdAndType(oAuth.id, OAuthType.KAKAO) } returns null
        every { userRepository.save(any<User>()) } answers { firstArg() }
        every { authRepository.save(any<Auth>()) } answers { firstArg() }

        authService.login(oAuthAccessToken, OAuthType.KAKAO, deviceToken = null, useMock = false)

        verify(exactly = 1) { oauthProcessorFactory.getClient(OAuthType.KAKAO, false) }
        verify(exactly = 0) { oauthProcessorFactory.getClient(OAuthType.KAKAO, true) }
    }

    test("동시 가입으로 Auth 저장이 unique 충돌하면 방금 만든 유저를 지우고 기존 계정으로 로그인한다") {
        val oAuthAccessToken = "kakao-access"
        val oAuth = OAuth(id = "oauth-race", type = OAuthType.KAKAO, nickName = "동시가입")
        val existingUserId = ObjectId()
        val existingUser = User(id = existingUserId, deviceToken = null, name = "동시가입")
        val existingAuth = Auth.create(oAuthId = oAuth.id, type = OAuthType.KAKAO, userId = existingUserId)
        val createdUser = slot<User>()

        every { oauthProcessorFactory.getClient(OAuthType.KAKAO, false) } returns oauthProcessor
        every { oauthProcessor.getOAuth(oAuthAccessToken) } returns oAuth
        every { authRepository.findByOAuthIdAndType(oAuth.id, OAuthType.KAKAO) } returnsMany listOf(null, existingAuth)
        every { userRepository.save(capture(createdUser)) } answers { firstArg() }
        every { authRepository.save(any<Auth>()) } throws DuplicateKeyException("E11000 duplicate key")
        every { userRepository.deleteById(any()) } returns Unit
        every { userRepository.findById(existingUserId) } returns Optional.of(existingUser)

        val result = authService.login(oAuthAccessToken, OAuthType.KAKAO, deviceToken = null, useMock = false)

        result.user.id shouldBe existingUserId
        verify(exactly = 1) { userRepository.deleteById(createdUser.captured.id) }
        verify(exactly = 0) { eventPublisher.publishEvent(any<UserRegisteredEvent>()) }
    }
})
