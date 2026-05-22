package notbe.tmtm.ddanddanserver.application.processor

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import notbe.tmtm.ddanddanserver.domain.exception.UnsupportedOAuthModeException
import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType

/**
 * OAuthProcessorFactory 단위 테스트.
 *
 * - `@PostConstruct init()`은 Spring이 호출하지만, 단위 테스트에서는 인스턴스 생성 후 직접 `init()`을 호출해 분류를 트리거한다.
 * - `clients.partition { it is MockOAuthProcessor }`로 real/mock이 분리되는지, 그리고 getClient의 분기를 검증한다.
 */
class OAuthProcessorFactoryTest : FunSpec({
    lateinit var realKakao: OAuthProcessor
    lateinit var realApple: OAuthProcessor
    lateinit var mockKakao: MockOAuthProcessor
    lateinit var mockApple: MockOAuthProcessor

    beforeEach {
        realKakao = mockk()
        realApple = mockk()
        mockKakao = mockk()
        mockApple = mockk()

        every { realKakao.getProviderType() } returns OAuthType.KAKAO
        every { realApple.getProviderType() } returns OAuthType.APPLE
        every { mockKakao.getProviderType() } returns OAuthType.KAKAO
        every { mockApple.getProviderType() } returns OAuthType.APPLE
    }

    test("real/mock processor가 함께 주입되면 마커 인터페이스 기준으로 정확히 분리된다") {
        val factory = OAuthProcessorFactory(listOf(realKakao, realApple, mockKakao, mockApple))
        factory.init()

        factory.getClient(OAuthType.KAKAO, useMock = false) shouldBeSameInstanceAs realKakao
        factory.getClient(OAuthType.APPLE, useMock = false) shouldBeSameInstanceAs realApple
        factory.getClient(OAuthType.KAKAO, useMock = true) shouldBeSameInstanceAs mockKakao
        factory.getClient(OAuthType.APPLE, useMock = true) shouldBeSameInstanceAs mockApple
    }

    test("useMock=false 요청 시 real processor가 반환된다") {
        val factory = OAuthProcessorFactory(listOf(realKakao, mockKakao))
        factory.init()

        val result = factory.getClient(OAuthType.KAKAO, useMock = false)

        result shouldBeSameInstanceAs realKakao
    }

    test("useMock=true이고 mock processor가 존재하면 mock processor가 반환된다") {
        val factory = OAuthProcessorFactory(listOf(realKakao, mockKakao))
        factory.init()

        val result = factory.getClient(OAuthType.KAKAO, useMock = true)

        result shouldBeSameInstanceAs mockKakao
    }

    test("useMock=true인데 mockMap에 해당 타입이 없으면 UnsupportedOAuthModeException이 발생한다 (prod 시나리오)") {
        // prod: real processor만 주입, mock processor는 빈으로 등록되지 않음
        val factory = OAuthProcessorFactory(listOf(realKakao, realApple))
        factory.init()

        val exception =
            shouldThrow<UnsupportedOAuthModeException> {
                factory.getClient(OAuthType.KAKAO, useMock = true)
            }

        exception.data shouldBe OAuthType.KAKAO
    }

    test("useMock=true && Apple mock 없음이면 data=APPLE로 UnsupportedOAuthModeException이 발생한다") {
        // KAKAO mock만 존재, APPLE mock 없는 상황
        val factory = OAuthProcessorFactory(listOf(realKakao, realApple, mockKakao))
        factory.init()

        val exception =
            shouldThrow<UnsupportedOAuthModeException> {
                factory.getClient(OAuthType.APPLE, useMock = true)
            }

        exception.data shouldBe OAuthType.APPLE
    }

    test("useMock=false인데 realMap에 해당 타입이 없으면 IllegalArgumentException이 발생한다") {
        // KAKAO real만 등록되고 APPLE real이 빠진 비정상 환경
        val factory = OAuthProcessorFactory(listOf(realKakao))
        factory.init()

        shouldThrow<IllegalArgumentException> {
            factory.getClient(OAuthType.APPLE, useMock = false)
        }
    }

    test("useMock=false 요청에서는 mockMap에 있어도 real이 없으면 IllegalArgumentException이 발생한다 - mock 우회 불가") {
        // mock만 등록된 상태에서 useMock=false 요청 — mock으로 fallback 되지 않아야 한다
        val factory = OAuthProcessorFactory(listOf(mockKakao))
        factory.init()

        shouldThrow<IllegalArgumentException> {
            factory.getClient(OAuthType.KAKAO, useMock = false)
        }
    }

    test("mock processor만 주입되어도 partition은 동작하며 useMock=true로 조회 가능하다") {
        // 가상 시나리오 - 실제로는 발생하지 않지만 partition의 견고성 검증
        val factory = OAuthProcessorFactory(listOf(mockKakao, mockApple))
        factory.init()

        factory.getClient(OAuthType.KAKAO, useMock = true) shouldBeSameInstanceAs mockKakao
        factory.getClient(OAuthType.APPLE, useMock = true) shouldBeSameInstanceAs mockApple
    }
})
