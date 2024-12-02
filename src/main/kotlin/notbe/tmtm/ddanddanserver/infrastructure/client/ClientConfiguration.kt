package notbe.tmtm.ddanddanserver.infrastructure.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory

@Configuration
class ClientConfiguration {
    @Bean
    fun slackClient(
        @Value("\${slack.hook-url}") hookUrl: String,
    ): SlackHookClient {
        val restClient =
            RestClient
                .builder()
                .baseUrl(hookUrl)
                .build()

        val restClientAdapter = RestClientAdapter.create(restClient)
        val factory = HttpServiceProxyFactory.builderFor(restClientAdapter).build()

        return factory.createClient(SlackHookClient::class.java)
    }
}
