package notbe.tmtm.ddanddanserver.config

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.springframework.boot.context.annotation.UserConfigurations
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

class AppVersionPropertiesTest : FunSpec({

    @Configuration
    @EnableConfigurationProperties(AppVersionProperties::class)
    class TestConfig

    val runner = ApplicationContextRunner()
        .withConfiguration(UserConfigurations.of(TestConfig::class.java))

    test("설정이 없으면 기본 최소 버전 1.5.0을 사용한다") {
        runner.run { context ->
            val properties = context.getBean(AppVersionProperties::class.java)
            properties.android shouldBe "1.5.0"
            properties.ios shouldBe "1.5.0"
        }
    }

    test("app.version.minimum 설정값을 플랫폼별로 바인딩한다") {
        runner
            .withPropertyValues("app.version.minimum.android=1.4.0", "app.version.minimum.ios=1.6.2")
            .run { context ->
                val properties = context.getBean(AppVersionProperties::class.java)
                properties.android shouldBe "1.4.0"
                properties.ios shouldBe "1.6.2"
            }
    }

    test("설정값 형식이 잘못되면 컨텍스트 기동이 실패한다") {
        runner
            .withPropertyValues("app.version.minimum.ios=1.5")
            .run { context ->
                context.startupFailure.shouldNotBeNullAndContain("app.version.minimum.ios")
            }
    }

    test("직접 생성 시에도 잘못된 형식이면 설정 키를 포함한 예외가 발생한다") {
        val exception = shouldThrow<IllegalArgumentException> {
            AppVersionProperties(android = "abc")
        }
        exception.message shouldContain "app.version.minimum.android"
    }
})

private fun Throwable?.shouldNotBeNullAndContain(text: String) {
    requireNotNull(this) { "컨텍스트 기동 실패가 예상되었지만 성공했습니다" }
    generateSequence(this) { it.cause }.any { it.message?.contains(text) == true } shouldBe true
}
