package notbe.tmtm.ddanddanserver.presentation.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class RestClientConfig {
    @Bean
    fun restTemplate(): RestClient = RestClient.create()
}
