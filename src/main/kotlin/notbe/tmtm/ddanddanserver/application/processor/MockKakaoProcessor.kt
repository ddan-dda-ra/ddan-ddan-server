package notbe.tmtm.ddanddanserver.application.processor

import notbe.tmtm.ddanddanserver.common.util.logger
import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "mock-oauth", name = ["enabled"], havingValue = "true")
class MockKakaoProcessor : MockOAuthProcessor {
    init {
        logger().warn(
            "MockKakaoProcessor 활성화 — dev/local profile 전용. 실제 카카오 API 호출 없이 accessToken을 시드로 사용한다.",
        )
    }

    override fun getProviderType(): OAuthType = OAuthType.KAKAO

    override fun getOAuth(accessToken: String): OAuth =
        OAuth(
            id = "mock-kakao-$accessToken",
            type = OAuthType.KAKAO,
            nickName = "MockKakao_$accessToken",
        )
}
