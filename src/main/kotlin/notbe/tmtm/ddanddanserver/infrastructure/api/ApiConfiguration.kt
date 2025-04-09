package notbe.tmtm.ddanddanserver.infrastructure.api

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory

@Configuration
class ApiConfiguration {
    @Bean
    fun slackHookApi(
        @Value("\${slack.hook-url}") hookUrl: String,
    ): SlackHookApi {
        val factory = HttpServiceProxyFactory.builderFor(restClientAdapter(hookUrl)).build()

        return factory.createClient(SlackHookApi::class.java)
    }

    @Bean
    fun kakaoAuthApi(): KakaoAuthApi {
        val factory = HttpServiceProxyFactory.builderFor(restClientAdapter()).build()

        return factory.createClient(KakaoAuthApi::class.java)
    }

    @Bean
    fun appleAuthApi(): AppleAuthApi {
        val factory = HttpServiceProxyFactory.builderFor(restClientAdapter()).build()

        return factory.createClient(AppleAuthApi::class.java)
    }

    private fun restClientAdapter(): RestClientAdapter =
        RestClientAdapter.create(
            RestClient
                .builder()
                .build(),
        )

    private fun restClientAdapter(baseUrl: String): RestClientAdapter =
        RestClientAdapter.create(
            RestClient
                .builder()
                .baseUrl(baseUrl)
                .build(),
        )
}
