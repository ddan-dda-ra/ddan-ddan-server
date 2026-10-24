package notbe.tmtm.ddanddanserver.config

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.context.annotation.UserConfigurations
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

class AppleOAuthPropertiesTest : FunSpec({

    @Configuration
    @EnableConfigurationProperties(AppleOAuthProperties::class)
    class TestConfig

    val runner = ApplicationContextRunner()
        .withConfiguration(UserConfigurations.of(TestConfig::class.java))

    test("설정이 없으면 기본 client id inc.ddanddan.DDanDDan을 사용한다") {
        runner.run { context ->
            context.getBean(AppleOAuthProperties::class.java).clientIds shouldBe listOf("inc.ddanddan.DDanDDan")
        }
    }

    test("oauth.apple.client-ids 설정값을 콤마 구분 목록으로 바인딩한다") {
        runner
            .withPropertyValues("oauth.apple.client-ids=inc.ddanddan.DDanDDan,inc.ddanddan.web")
            .run { context ->
                context.getBean(AppleOAuthProperties::class.java).clientIds shouldBe
                    listOf("inc.ddanddan.DDanDDan", "inc.ddanddan.web")
            }
    }

    test("client id 목록이 비어 있으면 예외가 발생한다") {
        shouldThrow<IllegalArgumentException> {
            AppleOAuthProperties(clientIds = emptyList())
        }
    }
})
