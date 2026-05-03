package notbe.tmtm.ddanddanserver.application.processor

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType

class MockKakaoProcessorTest : FunSpec({
    val processor = MockKakaoProcessor()

    test("provider type은 KAKAO이다") {
        processor.getProviderType() shouldBe OAuthType.KAKAO
    }

    test("accessToken을 시드로 OAuth 객체를 생성한다") {
        val oAuth = processor.getOAuth("user-001")

        oAuth.id shouldBe "mock-kakao-user-001"
        oAuth.type shouldBe OAuthType.KAKAO
        oAuth.nickName shouldBe "MockKakao_user-001"
    }

    test("같은 accessToken은 같은 id를 만들어 기존 사용자 로그인 흐름을 시뮬레이션한다") {
        val first = processor.getOAuth("same-token")
        val second = processor.getOAuth("same-token")

        first.id shouldBe second.id
    }

    test("다른 accessToken은 다른 id를 만들어 신규 사용자 가입 흐름을 시뮬레이션한다") {
        val first = processor.getOAuth("user-A")
        val second = processor.getOAuth("user-B")

        (first.id == second.id) shouldBe false
    }
})
