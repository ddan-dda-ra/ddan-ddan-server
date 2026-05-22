package notbe.tmtm.ddanddanserver.application.processor

import notbe.tmtm.ddanddanserver.common.util.logger
import notbe.tmtm.ddanddanserver.domain.model.auth.OAuthType
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "mock-oauth", name = ["enabled"], havingValue = "true")
class MockAppleProcessor : MockOAuthProcessor {
    init {
        logger().warn(
            "MockAppleProcessor 활성화 — dev/local profile 전용. 실제 Apple 키 검증 없이 accessToken을 시드로 사용한다.",
        )
    }

    override fun getProviderType(): OAuthType = OAuthType.APPLE

    override fun getOAuth(accessToken: String): OAuth =
        OAuth(
            id = "mock-apple-$accessToken",
            type = OAuthType.APPLE,
            nickName = "mock-$accessToken@example.com",
        )
}
