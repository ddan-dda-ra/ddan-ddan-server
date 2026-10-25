package notbe.tmtm.ddanddanserver.infrastructure.api

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import java.time.Duration

@Configuration
class ApiConfiguration(
    @Value("\${external-api.timeout.connect:3s}") private val connectTimeout: Duration,
    @Value("\${external-api.timeout.read:5s}") private val readTimeout: Duration,
) {
    @Bean
    fun slackHookApi(
        @Value("\${slack.hook-url}") hookUrl: String,
    ): SlackHookApi {
        val factory = HttpServiceProxyFactory.builderFor(restClientAdapter(hookUrl)).build()

        return factory.createClient(SlackHookApi::class.java)
    }

    @Bean
    fun discordHookApi(
        @Value("\${discord.hook-url}") hookUrl: String,
    ): DiscordHookApi {
        val factory = HttpServiceProxyFactory.builderFor(restClientAdapter(hookUrl)).build()

        return factory.createClient(DiscordHookApi::class.java)
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
            restClientBuilder()
                .build(),
        )

    private fun restClientAdapter(baseUrl: String): RestClientAdapter =
        RestClientAdapter.create(
            restClientBuilder()
                .baseUrl(baseUrl)
                .build(),
        )

    // 외부 API가 응답하지 않을 때 요청 스레드가 무기한 묶이지 않도록 타임아웃을 건다.
    private fun restClientBuilder(): RestClient.Builder =
        RestClient
            .builder()
            .requestFactory(
                SimpleClientHttpRequestFactory().apply {
                    setConnectTimeout(connectTimeout)
                    setReadTimeout(readTimeout)
                },
            )
}
