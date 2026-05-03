package notbe.tmtm.ddanddanserver.application.processor

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType

class MockAppleProcessorTest : FunSpec({
    val processor = MockAppleProcessor()

    test("provider type은 APPLE이다") {
        processor.getProviderType() shouldBe OAuthType.APPLE
    }

    test("accessToken을 시드로 OAuth 객체를 생성한다") {
        val oAuth = processor.getOAuth("user-001")

        oAuth.id shouldBe "mock-apple-user-001"
        oAuth.type shouldBe OAuthType.APPLE
        oAuth.nickName shouldBe "mock-user-001@example.com"
    }

    test("같은 accessToken은 같은 id를 만들어 기존 사용자 로그인 흐름을 시뮬레이션한다") {
        val first = processor.getOAuth("same-token")
        val second = processor.getOAuth("same-token")

        first.id shouldBe second.id
    }
})
